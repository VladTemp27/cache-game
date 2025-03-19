package main

import (
    "fmt"
    "log"
    "net/http"
    "sync"
    "encoding/json"
    "os"
    "time"
    "os/signal"
    "syscall"

    "github.com/gorilla/websocket"
)

// Map to store username to client ID mapping
var usernameToClientID = make(map[string]string)
var usernameMapMutex sync.Mutex

// Add this global variable at the top of the file with your other globals
var matchWorker *MatchWorker


// WebSocket upgrader
var upgrader = websocket.Upgrader{
    ReadBufferSize:  1024,
    WriteBufferSize: 1024,
    // Allow all origins for development
    CheckOrigin: func(r *http.Request) bool {
        return true
    },
}

// Client represents a connected WebSocket client
type Client struct {
    conn *websocket.Conn
    id   string
}

// ClientManager keeps track of all connected clients
type ClientManager struct {
    clients    map[string]*Client
    clientsMux sync.Mutex
}

// NewClientManager creates a new client manager
func NewClientManager() *ClientManager {
    return &ClientManager{
        clients: make(map[string]*Client),
    }
}

func (manager *ClientManager) addClient(client *Client) {
    manager.clientsMux.Lock()
    defer manager.clientsMux.Unlock()
    manager.clients[client.id] = client
    log.Printf("Client %s connected. Total clients: %d\n", client.id, len(manager.clients))
}

func (manager *ClientManager) removeClient(id string) {
    manager.clientsMux.Lock()
    defer manager.clientsMux.Unlock()
    delete(manager.clients, id)
    log.Printf("Client %s disconnected. Total clients: %d\n", id, len(manager.clients))
}

func main() {
    fmt.Println("Matchmaking service started on port 8085")

    // Test database connection on startup
    _, err := connectToMongoDB()
    if err != nil {
        log.Fatalf("Failed to connect to MongoDB on startup: %v", err)
    }
    
    manager := NewClientManager()
    clientCounter := 0

    // Create and start the match worker
    // Check for matches every 5 seconds with maximum score difference of 300
    matchWorker = NewMatchWorker(5*time.Second, 500, manager)
    matchWorker.Start()
    
    // Add a shutdown handler to gracefully stop the worker
    c := make(chan os.Signal, 1)
    signal.Notify(c, os.Interrupt, syscall.SIGTERM)
    go func() {
        <-c
        fmt.Println("Shutting down matchmaking service...")
        matchWorker.Stop()
        os.Exit(0)
    }()

    // HTTP endpoint for info
    http.HandleFunc("/", func(w http.ResponseWriter, r *http.Request) {
        fmt.Fprintf(w, "Matchmaking service - Connect via WebSocket at /ws")
    })
    
    // Add status endpoint
    http.HandleFunc("/status", func(w http.ResponseWriter, r *http.Request) {
        w.Header().Set("Content-Type", "application/json")
        json.NewEncoder(w).Encode(map[string]interface{}{
            "status":      "online",
            "matchesMade": matchWorker.matchesMade,
            "timestamp":   time.Now(),
        })
    })
    // WebSocket endpoint
    http.HandleFunc("/ws", func(w http.ResponseWriter, r *http.Request) {
        // Upgrade HTTP connection to WebSocket
        conn, err := upgrader.Upgrade(w, r, nil)
        if err != nil {
            log.Printf("Error upgrading to WebSocket: %v\n", err)
            return
        }

        // Create a new client
        clientCounter++
        clientID := fmt.Sprintf("client-%d", clientCounter)
        client := &Client{
            conn: conn,
            id:   clientID,
        }

        // Add client to manager
        manager.addClient(client)

        // Handle client connection in a goroutine
        go handleClient(client, manager)
    })

    // Start the server
    log.Fatal(http.ListenAndServe(":8085", nil))
}

func handleClient(client *Client, manager *ClientManager) {
    // Ensure we remove the client when the function exits
    defer func() {
        client.conn.Close()
        
        // Clean up username mapping
        usernameMapMutex.Lock()
        for username, clientID := range usernameToClientID {
            if clientID == client.id {
                delete(usernameToClientID, username)
                break
            }
        }
        usernameMapMutex.Unlock()
        
        manager.removeClient(client.id)
    }()

    // Send welcome message
    welcomeMsg := map[string]string{"type": "welcome", "message": "Welcome to the matchmaking service", "clientId": client.id}
    if err := client.conn.WriteJSON(welcomeMsg); err != nil {
        log.Printf("Error sending welcome message: %v", err)
        return
    }

    // Message handling loop
    for {
        // Read message from the WebSocket
        messageType, message, err := client.conn.ReadMessage()
        if err != nil {
            log.Printf("Error reading message: %v", err)
            break
        }

        if messageType != websocket.TextMessage {
            log.Printf("Received non-text message from %s: %v", client.id, messageType)
            break
        }

        // Parse JSON
        var jsonMsg map[string]interface{}
        if err := json.Unmarshal(message, &jsonMsg); err != nil {
            log.Printf("Error parsing message: %v", err)
            continue
        } else {
            log.Print("Printing message:")
            log.Printf("JSON Message %v: ", jsonMsg) 
        }

        if msgType, ok := jsonMsg["type"].(string); !ok || msgType == "" {
            log.Printf("Invalid message type")
            sendErrorResponse(client, "Invalid message type")
            continue
        }

        // Process the message based on its type
        msgType := jsonMsg["type"].(string)
        processMessage(client, msgType, jsonMsg)

        // Log the received message
        log.Printf("Received message from %s: %s", client.id, string(message))
    }
}

// Process incoming messages based on type
func processMessage(client *Client, msgType string, jsonMsg map[string]interface{}) {
    switch msgType {
    case "queue":
        handleQueueRequest(client, jsonMsg)
    case "cancel":
        handleCancelRequest(client, jsonMsg)
    case "received":
        log.Printf("Client %s received message", client.id)
        // This case is for acknowledgment, nothing to do
    default:
        log.Printf("Invalid message type: %s", msgType)
        sendErrorResponse(client, "Invalid message type")
    }
}

// Handle queue request
func handleQueueRequest(client *Client, jsonMsg map[string]interface{}) {
    log.Printf("Client %s is queuing", client.id)
    
    if !checkValidUser(jsonMsg) {
        log.Printf("Invalid JSON")
        sendErrorResponse(client, "Invalid JSON format, please check the fields")
        return
    }

    // Convert score to int
    score, ok := jsonMsg["score"].(float64)
    if !ok {
        log.Printf("Invalid score format")
        sendErrorResponse(client, "Invalid score format")
        return
    }
    
    // Store username to client ID mapping for match notifications
    username := jsonMsg["username"].(string)
    usernameMapMutex.Lock()
    usernameToClientID[username] = client.id
    usernameMapMutex.Unlock()
    
    // Creates a user structure
    player := QueuePlayer{
        Username: jsonMsg["username"].(string),
        Token:    jsonMsg["token"].(string),
        Score:    int(score),
        Cluster:  os.Getenv("CLUSTER"),
        JoinedAt: time.Now(),
    }
    
    // Add user to queue via MongoDB
    err := AddPlayerToQueue(&player)
    if err != nil {
        log.Printf("Error adding player to queue: %v", err)
        sendErrorResponse(client, "Failed to add player to queue")
        return
    }
    
    // Send success response
    client.conn.WriteJSON(map[string]string{
        "type": "queue_success",
        "message": "You have been added to the matchmaking queue",
    })
}

// Handle cancel request
func handleCancelRequest(client *Client, jsonMsg map[string]interface{}) {
    log.Printf("Client %s is cancelling", client.id)
    
    if !checkValidUser(jsonMsg) {
        log.Printf("Invalid JSON")
        sendErrorResponse(client, "Invalid JSON format, please check the fields")
        return
    }
    
    username := jsonMsg["username"].(string)
    token := jsonMsg["token"].(string)
    
    // Remove client from queue
    err := RemovePlayerFromQueue(username, token)
    if err != nil {
        log.Printf("Error removing player from queue: %v", err)
        sendErrorResponse(client, "Failed to remove player from queue: "+err.Error())
        return
    }
    
    // Send success response
    client.conn.WriteJSON(map[string]string{
        "type": "cancel_success",
        "message": "You have been removed from the matchmaking queue",
    })
}

// Send error response to client
func sendErrorResponse(client *Client, message string) {
    client.conn.WriteJSON(map[string]string{
        "type": "error",
        "message": message,
    })
}

// Check if the JSON message is valid for queueing
func checkValidUser(jsonMessage map[string]interface{}) bool {
    if jsonMessage["username"] == nil || jsonMessage["token"] == nil || jsonMessage["score"] == nil {
        return false
    }

    _, usernameOk := jsonMessage["username"].(string)
    _, tokenOk := jsonMessage["token"].(string)
    
    // Score can be either int or float64 (JSON numbers are parsed as float64)
    _, scoreIntOk := jsonMessage["score"].(int)
    _, scoreFloatOk := jsonMessage["score"].(float64)

    log.Print("Printing values of usernameOk, tokenOk, scoreIntOk, scoreFloatOk: ")
    log.Printf("%v, %v, %v, %v", usernameOk, tokenOk, scoreIntOk, scoreFloatOk)
    
    return usernameOk && tokenOk && (scoreIntOk || scoreFloatOk)
}