package main

import (
    "context"
    "crypto/rand"
    "fmt"
    "log"
    "math"
    "sync"
    "time"

    "go.mongodb.org/mongo-driver/bson"
    "go.mongodb.org/mongo-driver/mongo"
    "go.mongodb.org/mongo-driver/mongo/options"
    "github.com/gorilla/websocket"
)

// Match represents a pair of matched players
type Match struct {
    PlayerA   QueuePlayer `bson:"playerA" json:"playerA"`
    PlayerB   QueuePlayer `bson:"playerB" json:"playerB"`
    MatchedAt time.Time   `bson:"matchedAt" json:"matchedAt"`
    RoomID    string      `bson:"roomId" json:"roomId"`
}

// MatchWorker handles the matchmaking process
type MatchWorker struct {
    interval     time.Duration
    maxScoreDiff int
    running      bool
    stopChan     chan struct{}
    matchesMade  int
    mutex        sync.Mutex
    manager      *ClientManager
    dbClient     *mongo.Client // Store a persistent MongoDB client
}

// NewMatchWorker creates a new match worker
func NewMatchWorker(interval time.Duration, maxScoreDiff int, manager *ClientManager) *MatchWorker {
    // Create a persistent MongoDB connection that will be reused
    client, err := connectToMongoDB()
    if err != nil {
        log.Fatalf("Failed to connect to MongoDB for match worker: %v", err)
    }

    return &MatchWorker{
        interval:     interval,
        maxScoreDiff: maxScoreDiff,
        running:      false,
        stopChan:     make(chan struct{}),
        manager:      manager,
        dbClient:     client,
    }
}

// Start begins the matchmaking process
func (w *MatchWorker) Start() {
    w.mutex.Lock()
    defer w.mutex.Unlock()

    if w.running {
        log.Println("Match worker already running")
        return
    }

    // Clear queue at start
    if err := w.clearMatchmakingQueue(); err != nil{
        log.Printf("Failed to clear matchmaking queue: %v", err)
    }else{
        fmt.Println("Matchmaking queue cleared")
    }

    w.running = true
    w.stopChan = make(chan struct{})
    go w.run()

    log.Println("Match worker started")
}

// Stop halts the matchmaking process
func (w *MatchWorker) Stop() {
    w.mutex.Lock()
    defer w.mutex.Unlock()

    if !w.running {
        return
    }

    close(w.stopChan)
    w.running = false
    
    // Properly close the MongoDB connection
    if w.dbClient != nil {
        ctx, cancel := context.WithTimeout(context.Background(), 5*time.Second)
        defer cancel()
        w.dbClient.Disconnect(ctx)
    }
    
    log.Printf("Match worker stopped, total matches made: %d", w.matchesMade)
}

// run is the main loop for the worker
func (w *MatchWorker) run() {
    ticker := time.NewTicker(w.interval)
    defer ticker.Stop()

    for {
        select {
        case <-w.stopChan:
            return
        case <-ticker.C:
            if err := w.findMatches(); err != nil {
                log.Printf("Error finding matches: %v", err)
                
                // Check if we need to reconnect to MongoDB
                if w.dbClient == nil {
                    client, err := connectToMongoDB()
                    if err != nil {
                        log.Printf("Failed to reconnect to MongoDB: %v", err)
                        continue
                    }
                    w.dbClient = client
                }
            }
        }
    }
}

// generateRoomID creates a unique room ID for the match
func generateRoomID() string {
    b := make([]byte, 8)
    _, err := rand.Read(b)
    if err != nil {
        // Fallback to timestamp if crypto fails
        return fmt.Sprintf("r-%d", time.Now().UnixNano())
    }
    return fmt.Sprintf("r-%x", b)
}

// findMatches searches for suitable player matches
func (w *MatchWorker) findMatches() error {
    // Use the persistent MongoDB client instead of creating a new one each time
    if w.dbClient == nil {
        return fmt.Errorf("MongoDB client is nil")
    }

    // Check if the client is still connected
    ctx, cancel := context.WithTimeout(context.Background(), 2*time.Second)
    defer cancel()
    
    if err := w.dbClient.Ping(ctx, nil); err != nil {
        log.Printf("MongoDB ping failed, reconnecting: %v", err)
        
        // Try to reconnect
        client, err := connectToMongoDB()
        if err != nil {
            return fmt.Errorf("failed to reconnect to MongoDB: %v", err)
        }
        w.dbClient = client
    }

    if err := w.removeTimedOutPlayers(); err != nil {
        fmt.Printf("failed to remove timed out players: %v", err)
    }

    // Get all players in the queue, sorted by join time (oldest first)
    collection := w.dbClient.Database("cache_db").Collection("matchmaking")
    findOptions := options.Find().SetSort(bson.M{"joinedAt": 1})
    
    ctx, cancel = context.WithTimeout(context.Background(), 10*time.Second)
    defer cancel()
    
    cursor, err := collection.Find(ctx, bson.M{}, findOptions)
    if err != nil {
        return fmt.Errorf("failed to query queue: %v", err)
    }
    defer cursor.Close(ctx)

    var players []QueuePlayer
    if err = cursor.All(ctx, &players); err != nil {
        return fmt.Errorf("failed to decode players: %v", err)
    }

    if len(players) < 2 {
        // Not enough players to make a match
        return nil
    }

    log.Printf("Processing queue with %d players", len(players))
    
    // Process players to find matches
    processed := make(map[string]bool)
    
    for i, playerA := range players {
        if processed[playerA.Username] {
            continue
        }
        
        bestMatch := -1
        bestScoreDiff := math.MaxFloat64
        
        // Find the best match for this player
        for j, playerB := range players {
            if i == j || processed[playerB.Username] {
                continue
            }
            
            // Calculate score difference
            scoreDiff := math.Abs(float64(playerA.Score - playerB.Score))
            
            // If the score difference is too large, skip
            if int(scoreDiff) > w.maxScoreDiff {
                continue
            }
            
            // Find the closest match in terms of score
            if scoreDiff < bestScoreDiff {
                bestMatch = j
                bestScoreDiff = scoreDiff
            }
        }
        
        // If we found a suitable match
        if bestMatch != -1 {
            playerB := players[bestMatch]
            
            // Generate a room ID for this match
            roomID := generateRoomID()
            
            // Create a match
            match := Match{
                PlayerA:   playerA,
                PlayerB:   playerB,
                MatchedAt: time.Now(),
                RoomID:    roomID,
            }
            
            // Save the match without using transactions
            if err := w.saveMatchSafely(match); err != nil {
                log.Printf("Failed to save match: %v", err)
                continue
            }
            
            // Notify players about the match
            w.notifyPlayersAndCloseConnections(match)
            
            // Mark these players as processed
            processed[playerA.Username] = true
            processed[playerB.Username] = true
            
            // Update match count
            w.mutex.Lock()
            w.matchesMade++
            w.mutex.Unlock()
            
            log.Printf("Match created: %s vs %s (score diff: %.0f), Room ID: %s",
                playerA.Username, playerB.Username, bestScoreDiff, roomID)
        }
    }
    
    return nil
}

// saveMatchSafely stores the match and removes players from the queue without using transactions
func (w *MatchWorker) saveMatchSafely(match Match) error {
    // First, insert the match
    ctx1, cancel1 := context.WithTimeout(context.Background(), 5*time.Second)
    defer cancel1()
    
    matchesCollection := w.dbClient.Database("cache_db").Collection("matches")
    _, err := matchesCollection.InsertOne(ctx1, match)
    if err != nil {
        return fmt.Errorf("failed to insert match: %v", err)
    }
    
    // Then, remove the players from the queue
    ctx2, cancel2 := context.WithTimeout(context.Background(), 5*time.Second)
    defer cancel2()
    
    queueCollection := w.dbClient.Database("cache_db").Collection("matchmaking")
    _, err = queueCollection.DeleteMany(ctx2, bson.M{
        "username": bson.M{
            "$in": []string{match.PlayerA.Username, match.PlayerB.Username},
        },
    })
    if err != nil {
        // Note: We already saved the match, but couldn't remove players from queue.
        // This is not ideal but at least the match is recorded.
        log.Printf("Warning: Match saved but couldn't remove players from queue: %v", err)
        return fmt.Errorf("failed to remove players from queue: %v", err)
    }
    
    return nil
}

// notifyPlayersAndCloseConnections sends match notifications to both players and closes their connections
func (w *MatchWorker) notifyPlayersAndCloseConnections(match Match) {
    // Get clients for both players
    clientA := w.getClientForUsername(match.PlayerA.Username)
    clientB := w.getClientForUsername(match.PlayerB.Username)
    
    // Notify and close player A's connection
    if clientA != nil {
        w.notifyAndCloseConnection(clientA, match, match.PlayerB)
    }
    
    // Notify and close player B's connection
    if clientB != nil {
        w.notifyAndCloseConnection(clientB, match, match.PlayerA)
    }
}

// getClientForUsername retrieves the client connection for a username
func (w *MatchWorker) getClientForUsername(username string) *Client {
    // Get the client ID for this username
    usernameMapMutex.Lock()
    clientID, exists := usernameToClientID[username]
    usernameMapMutex.Unlock()
    
    if !exists {
        log.Printf("Client for username %s not found", username)
        return nil
    }
    
    // Get the client
    w.manager.clientsMux.Lock()
    client, exists := w.manager.clients[clientID]
    w.manager.clientsMux.Unlock()
    
    if !exists {
        log.Printf("Client %s not connected", clientID)
        return nil
    }
    
    return client
}

// Fix the error in the notifyAndCloseConnection function

// notifyAndCloseConnection sends a match notification to a player and closes their connection
func (w *MatchWorker) notifyAndCloseConnection(client *Client, match Match, opponent QueuePlayer) {
    // Create match notification
    matchNotification := map[string]interface{}{
        "type":      "match_found",
        "opponent":  opponent.Username,
        "score":     opponent.Score,
        "roomId":    match.RoomID,
        "timestamp": match.MatchedAt,
        "message":   "Connection will close after this message. Please join the game room.",
    }
    
    // Send the notification
    if err := client.conn.WriteJSON(matchNotification); err != nil {
        log.Printf("Error notifying player: %v", err)
    } else {
        // Give client a short time to process the message before closing
        time.Sleep(500 * time.Millisecond)
        
        // Send a close message
        closeMsg := map[string]interface{}{
            "type":    "connection_closing",
            "message": "Match found. Please join the game room.",
            "roomId":  match.RoomID,
        }
        client.conn.WriteJSON(closeMsg)
        
        // Close the connection
        err := client.conn.WriteMessage(
            websocket.CloseMessage, 
            websocket.FormatCloseMessage(websocket.CloseNormalClosure, "Match found"),
        )
        if err != nil {
            log.Printf("Error sending close frame: %v", err)
        }
        
        // Close the underlying connection
        client.conn.Close()
        
        log.Printf("Notified player about match and closed connection")
        
        // Clean up client from manager
        go func(clientID string) {
            // Give some time for the WebSocket close to be processed
            time.Sleep(1 * time.Second)
            
            // Clean up username mapping
            usernameMapMutex.Lock()
            for username, id := range usernameToClientID {
                if id == clientID {
                    delete(usernameToClientID, username)
                    break
                }
            }
            usernameMapMutex.Unlock()
            
            // Remove client from manager
            w.manager.removeClient(clientID)
        }(client.id)
    }
}

func (w *MatchWorker) removeTimedOutPlayers() error{
    // Calculate the cutoff time (30 seconds ago)
    // Note since the tick rate is 5 seconds this will cut off on 35 seconds in actuality
    timeoutCutoff := time.Now().Add(-30 * time.Second)
        
    ctx, cancel := context.WithTimeout(context.Background(), 5*time.Second)
    defer cancel()

    collection := w.dbClient.Database("cache_db").Collection("matchmaking")

    // Find players to timeout so we can notify them
    filter := bson.M{"joinedAt": bson.M{"$lt": timeoutCutoff}}
    cursor, err := collection.Find(ctx, filter)
    if err != nil {
        return fmt.Errorf("failed to query timed out players: %v", err)
    }

    var timedOutPlayers []QueuePlayer
    if err = cursor.All(ctx, &timedOutPlayers); err != nil {
        cursor.Close(ctx)
        return fmt.Errorf("failed to decode timed out players: %v", err)
    }
    cursor.Close(ctx)

    if len(timedOutPlayers) == 0 {
        // No timed out players
        return nil
    }

    // Delete timed out players from the queue
    deleteResult, err := collection.DeleteMany(ctx, filter)
    if err != nil {
        return fmt.Errorf("failed to remove timed out players: %v", err)
    }

    // Notify timed out players
    for _, player := range timedOutPlayers {
        w.notifyPlayerTimeout(player.Username)
    }

    log.Printf("Removed %d players from queue due to timeout", deleteResult.DeletedCount)
    return nil

}

func (w *MatchWorker) notifyPlayerTimeout(username string) {
    client := w.getClientForUsername(username)
    if client == nil {
        return
    }
    
    // Create timeout notification
    timeoutNotification := map[string]interface{}{
        "type":    "queue_timeout",
        "message": "You have been removed from the queue after waiting for 30 seconds",
    }
    
    // Send the notification
    if err := client.conn.WriteJSON(timeoutNotification); err != nil {
        log.Printf("Error notifying player about timeout: %v", err)
    }
    
    // Give client a short time to process the message before closing
    time.Sleep(500 * time.Millisecond)
    
    // Close the connection
    err := client.conn.WriteMessage(
        websocket.CloseMessage, 
        websocket.FormatCloseMessage(websocket.CloseNormalClosure, "Queue timeout"),
    )
    if err != nil {
        log.Printf("Error sending close frame: %v", err)
    }
    
    // Close the underlying connection
    client.conn.Close()
    
    log.Printf("Notified player %s about queue timeout and closed connection", username)
    
    // Clean up client from manager (similar to match notification)
    go func(clientID string) {
        // Give some time for the WebSocket close to be processed
        time.Sleep(1 * time.Second)
        
        // Clean up username mapping
        usernameMapMutex.Lock()
        for u, id := range usernameToClientID {
            if id == clientID {
                delete(usernameToClientID, u)
                break
            }
        }
        usernameMapMutex.Unlock()
        
        // Remove client from manager
        w.manager.removeClient(clientID)
    }(client.id)
}

// ClearMatchmakingQueue removes all players from the matchmaking queue
func (w *MatchWorker) clearMatchmakingQueue() error {
    // Create context with timeout
    ctx, cancel := context.WithTimeout(context.Background(), 10*time.Second)
    defer cancel()
    
    // Get the matchmaking collection
    collection := w.dbClient.Database("cache_db").Collection("matchmaking")
    
    // Find all players first (to notify them)
    cursor, err := collection.Find(ctx, bson.M{})
    if err != nil {
        return fmt.Errorf("failed to query queue players: %v", err)
    }
    
    var queuedPlayers []QueuePlayer
    if err = cursor.All(ctx, &queuedPlayers); err != nil {
        cursor.Close(ctx)
        return fmt.Errorf("failed to decode queued players: %v", err)
    }
    cursor.Close(ctx)
    
    // Delete all documents in the collection
    result, err := collection.DeleteMany(ctx, bson.M{})
    if err != nil {
        return fmt.Errorf("failed to clear matchmaking queue: %v", err)
    }
    
    log.Printf("Matchmaking queue cleared: %d players removed", result.DeletedCount)
    
    // Notify all players who were in the queue
    for _, player := range queuedPlayers {
        go w.notifyQueueCleared(player.Username)
    }
    
    return nil
}

func (w *MatchWorker) notifyQueueCleared(username string) {
    client := w.getClientForUsername(username)
    if client == nil {
        return
    }
    
    // Create queue cleared notification
    notification := map[string]interface{}{
        "type":    "queue_cleared",
        "message": "The matchmaking queue has been cleared by the system",
    }
    
    // Send the notification
    if err := client.conn.WriteJSON(notification); err != nil {
        log.Printf("Error notifying player about queue clear: %v", err)
    }
    
    // Give client a short time to process the message before closing
    time.Sleep(500 * time.Millisecond)
    
    // Close the connection
    err := client.conn.WriteMessage(
        websocket.CloseMessage, 
        websocket.FormatCloseMessage(websocket.CloseNormalClosure, "Queue cleared"),
    )
    if err != nil {
        log.Printf("Error sending close frame: %v", err)
    }
    
    // Close the underlying connection
    client.conn.Close()
    
    log.Printf("Notified player %s about queue clear and closed connection", username)
    
    // Clean up client from manager
    go func(clientID string) {
        // Give some time for the WebSocket close to be processed
        time.Sleep(1 * time.Second)
        
        // Clean up username mapping
        usernameMapMutex.Lock()
        for u, id := range usernameToClientID {
            if id == clientID {
                delete(usernameToClientID, u)
                break
            }
        }
        usernameMapMutex.Unlock()
        
        // Remove client from manager
        w.manager.removeClient(clientID)
    }(client.id)
}

// Deprecated: use notifyPlayersAndCloseConnections instead
func (w *MatchWorker) notifyPlayers(match Match) {
    log.Println("Warning: Using deprecated notifyPlayers method")
    w.notifyPlayersAndCloseConnections(match)
}

// Deprecated: use notifyAndCloseConnection instead
func (w *MatchWorker) notifyPlayer(username string, match Match) {
    log.Println("Warning: Using deprecated notifyPlayer method")
    client := w.getClientForUsername(username)
    if client == nil {
        return
    }
    
    // Determine the opponent
    var opponent QueuePlayer
    if match.PlayerA.Username == username {
        opponent = match.PlayerB
    } else {
        opponent = match.PlayerA
    }
    
    w.notifyAndCloseConnection(client, match, opponent)
}