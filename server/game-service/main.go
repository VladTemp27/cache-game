package main

// Import statements
import (
	"encoding/json"
	"fmt"
	"io"
	"math/rand"
	"net/http"
	"os"
	"sync"
	"time"

	"github.com/gorilla/websocket"
)

// Card struct represents a card with a question and answer pair
type Card struct {
	PairID int `json:"pair_id"`
	Pair   struct {
		Question string `json:"question"`
		Answer   string `json:"answer"`
	} `json:"pair"`
}

// Game struct represents a single game instance
type Game struct {
	Mutex         sync.Mutex
	Players       []*websocket.Conn // tracks the players in a game
	Usernames     [2]string         // tracks the usernames of both players
	Scores        []int             // tracks the scores of both players
	CurrentPlayer int               // indicates the current player's turn
	Timer         int               // tracks the time left in a game
	Round         int               // tracks the number of rounds in a game
	LoopRunning   bool              // indicates if the game loop is running
	Cards         [16]string        // stores the questions and answers
	PairIDs       [16]int           // stores the pair IDs for matching
	FlippedCard   int               // stores the index of the first flipped card
	Paired        [16]bool          // tracks which cards have been paired
	GameStatus    string            // tracks the status of the game
	WhoseTurn     string            // indicates whose turn it is
	Winner        int               // indicates the winner of the game
}

// Global variables
var (
	games    = make(map[string]*Game) // store active games
	gamesMux sync.Mutex
	upgrader = websocket.Upgrader{
		CheckOrigin: func(r *http.Request) bool { return true },
	}
	rng = rand.New(rand.NewSource(time.Now().UnixNano()))
)

// Function to read cards from the JSON file
func readCardsFromFile(filename string) ([]Card, error) {
	file, err := os.Open(filename)
	if err != nil {
		return nil, err
	}
	defer file.Close()

	bytes, err := io.ReadAll(file)
	if err != nil {
		return nil, err
	}

	var cards []Card
	if err := json.Unmarshal(bytes, &cards); err != nil {
		return nil, err
	}

	return cards, nil
}

// Function to fetch random 8 IDs from the JSON file and store them randomly in indexes 0-15
func fetchAndStoreQuestions(game *Game) error {
	cards, err := readCardsFromFile("./cards.json")
	if err != nil {
		return err
	}

	// Shuffle and select 8 random cards
	rand.Shuffle(len(cards), func(i, j int) { cards[i], cards[j] = cards[j], cards[i] })
	selectedCards := cards[:8]

	// Shuffle and store questions and answers in indexes 0-15
	indices := rand.Perm(16)
	for i, card := range selectedCards {
		question := card.Pair.Question
		answer := card.Pair.Answer
		pairID := card.PairID

		game.Cards[indices[i*2]] = question
		game.Cards[indices[i*2+1]] = answer
		game.PairIDs[indices[i*2]] = pairID
		game.PairIDs[indices[i*2+1]] = pairID

		// Log the assignment
		fmt.Printf("[ASSIGN] Index %d: Question: %s, Pair ID: %d\n", indices[i*2], question, pairID)
		fmt.Printf("[ASSIGN] Index %d: Answer: %s, Pair ID: %d\n", indices[i*2+1], answer, pairID)
	}

	return nil
}

// Function to start a game with the specified game ID
func startGame(gameID string) {
	gamesMux.Lock()
	game, exists := games[gameID]
	if !exists {
		gamesMux.Unlock()
		fmt.Println("[ERROR] Game not found:", gameID)
		return
	}
	if game.LoopRunning {
		gamesMux.Unlock()
		fmt.Println("[INFO] Game loop already running for:", gameID)
		return
	}
	game.LoopRunning = true
	game.GameStatus = "players_ready"
	gamesMux.Unlock()

	if err := fetchAndStoreQuestions(game); err != nil {
		fmt.Println("[ERROR] Failed to fetch and store questions:", err)
		return
	}

	// Use the new random generator instance
	game.CurrentPlayer = rng.Intn(2) // randomly select the starting player
	game.Round = 1                   // start at round 1

	// Set the timer to 3 minutes
	game.Timer = 180
	game.LoopRunning = true

	fmt.Printf("[GAME START] Game ID: %s | Timer: %d seconds | %s starts (Round %d)\n", gameID, game.Timer, game.Usernames[game.CurrentPlayer], game.Round)

	// Send game ready information to players
	sendGameReadyEvent(game, gameID)

	go gameLoop(game, gameID)
}

func gameLoop(game *Game, gameID string) {
	ticker := time.NewTicker(1 * time.Second)
	defer ticker.Stop()

	// Notify players that the game is ready and both players are connected
	sendPlayersReadyEvent(game, gameID)

	for game.Timer > 0 {
		<-ticker.C

		game.Mutex.Lock()
		if game.GameStatus == "game_end" {
			game.Mutex.Unlock()
			fmt.Printf("[GAME OVER] Game ID: %s\n", gameID)
			return
		}
		if game.Timer > 0 {
			game.Timer--
		}
		fmt.Printf("[TIMER] Game ID: %s | Time Left: %d seconds | Round: %d | Scores: [%d, %d]\n", gameID, game.Timer, game.Round, game.Scores[0], game.Scores[1])

		game.Mutex.Unlock()
	}

	// Timer has run out
	game.Mutex.Lock()
	game.GameStatus = "game_end"
	game.LoopRunning = false
	game.Mutex.Unlock()

	// Determine the winner
	winner := -1
	if game.Scores[0] > game.Scores[1] {
		winner = 0
	} else if game.Scores[1] > game.Scores[0] {
		winner = 1
	}
	game.Winner = winner

	// Send game end event
	sendGameEndEvent(game, gameID)

	if winner != -1 {
		fmt.Printf("[TIME UP] Game ID: %s | Game Over! %s wins with %d points\n", gameID, game.Usernames[winner], game.Scores[winner])
	} else {
		fmt.Printf("[TIME UP] Game ID: %s | Game Over! It's a tie with %d points each\n", gameID, game.Scores[0])
	}
}

// Function to handle new WebSocket connections
func handleConnection(w http.ResponseWriter, r *http.Request) {
	conn, err := upgrader.Upgrade(w, r, nil)
	if err != nil {
		fmt.Println("[ERROR] WebSocket upgrade failed:", err)
		return
	}

	gameID := r.URL.Query().Get("gameID")
	player := r.URL.Query().Get("player")
	username := r.URL.Query().Get("username")

	if gameID == "" || player == "" || username == "" {
		fmt.Println("[ERROR] Missing gameID, player, or username parameter")
		conn.Close()
		return
	}

	gamesMux.Lock()
	game, exists := games[gameID]
	if !exists {
		// Create a new game if it doesn't exist
		games[gameID] = &Game{
			Players:     make([]*websocket.Conn, 2),
			Usernames:   [2]string{"", ""},
			Scores:      make([]int, 2),
			FlippedCard: -1,
			GameStatus:  "game_ready",
		}
		game = games[gameID]
		fmt.Printf("[NEW GAME] Created game %s\n", gameID)
	}
	gamesMux.Unlock()

	game.Mutex.Lock()
	defer game.Mutex.Unlock()

	// Check if the player is reconnecting
	var playerIdx int = -1
	for i := 0; i < 2; i++ {
		if game.Usernames[i] == username {
			playerIdx = i
			break
		}
	}

	if playerIdx == -1 {
		// If the player is not reconnecting, find the first available slot
		for i := 0; i < 2; i++ {
			if game.Players[i] == nil && game.Usernames[i] == "" {
				playerIdx = i
				game.Usernames[i] = username
				break
			}
		}
	}

	if playerIdx == -1 {
		// No available slot for the player
		fmt.Println("[ERROR] Game already has 2 players, rejecting connection...")
		conn.Close()
		return
	}

	// Check if the slot is already occupied
	if game.Players[playerIdx] != nil {
		fmt.Printf("[ERROR] %s is already connected in Game ID: %s\n", username, gameID)
		conn.Close()
		return
	}

	// Connect or reconnect the player
	game.Players[playerIdx] = conn
	fmt.Printf("[CONNECTED] %s joined Game ID: %s\n", username, gameID)

	// Start the game when both players are connected
	if game.Players[0] != nil && game.Players[1] != nil && game.GameStatus == "game_ready" {
		game.GameStatus = "players_ready"
		startGame(gameID)
	}

	// Monitor the connection and listen for messages
	go monitorConnection(game, conn, playerIdx, username, gameID)
	go listenForMessages(conn, game, playerIdx, username, gameID)
}

// Function to keep-alive pings
func monitorConnection(game *Game, conn *websocket.Conn, playerIdx int, username, gameID string) {
	pingTicker := time.NewTicker(5 * time.Second)
	defer pingTicker.Stop()

	for {
		select {
		case <-pingTicker.C:
			game.Mutex.Lock()
			if game.GameStatus == "game_end" {
				game.Mutex.Unlock()
				return
			}
			if err := conn.WriteMessage(websocket.PingMessage, nil); err != nil {
				fmt.Printf("[DISCONNECTED] %s lost connection | Game ID: %s\n", username, gameID)
				game.Players[playerIdx] = nil
				conn.Close()
				game.Mutex.Unlock()
				handleDisconnection(gameID, playerIdx)
				return
			}
			game.Mutex.Unlock()
		}
	}
}

// Function to handle player disconnection
func handleDisconnection(gameID string, playerIdx int) {
	gamesMux.Lock()
	game, exists := games[gameID]
	gamesMux.Unlock()
	if !exists {
		return
	}

	game.Mutex.Lock()
	defer game.Mutex.Unlock()

	if game.GameStatus == "game_end" {
		return
	}

	fmt.Printf("[WAITING] %s disconnected | Waiting 10s before declaring other player winner | Game ID: %s\n", game.Usernames[playerIdx], gameID)

	go handlePlayerTimeout(game, playerIdx, gameID)
}

// Function to handle the player timeout
func handlePlayerTimeout(game *Game, playerIdx int, gameID string) {
	time.Sleep(10 * time.Second)
	game.Mutex.Lock()
	defer game.Mutex.Unlock()

	if game.Players[playerIdx] == nil {
		game.GameStatus = "game_end"
		winner := 1 - playerIdx
		game.Winner = winner // Set the winner
		fmt.Printf("[WIN] %s wins by default | Game ID: %s\n", game.Usernames[winner], gameID)

		// Send game end event
		sendGameEndEvent(game, gameID)
	}
}

// Function to listen for messages from client
func listenForMessages(conn *websocket.Conn, game *Game, playerIdx int, username, gameID string) {
	defer conn.Close()
	for {
		_, message, err := conn.ReadMessage()
		if err != nil {
			fmt.Printf("[DISCONNECTED] %s closed connection | Game ID: %s\n", username, gameID)
			game.Mutex.Lock()
			game.Players[playerIdx] = nil
			game.Mutex.Unlock()
			handleDisconnection(gameID, playerIdx)
			return
		}

		var payload struct {
			Action    string `json:"action"`
			Matched   bool   `json:"matched"`
			CardIndex int    `json:"cardIndex"`
		}
		if err := json.Unmarshal(message, &payload); err == nil {
			switch payload.Action {
			case "move":
				handleMove(gameID, playerIdx, payload.Matched)
			case "quit":
				handleQuit(gameID, playerIdx)
			case "flip":
				handleFlip(gameID, playerIdx, payload.CardIndex)
			}
		}
	}
}

// Function to handle a player's move
func handleMove(gameID string, playerIndex int, matched bool) {
	gamesMux.Lock()
	game, exists := games[gameID]
	gamesMux.Unlock()
	if !exists {
		fmt.Println("[ERROR] Game not found:", gameID)
		return
	}

	game.Mutex.Lock()
	defer game.Mutex.Unlock()

	if game.GameStatus == "game_end" {
		fmt.Println("[INFO] Move ignored, game is over")
		return
	}

	if game.CurrentPlayer != playerIndex {
		fmt.Printf("[WARNING] %s tried to move out of turn in Game ID: %s\n", game.Usernames[playerIndex], gameID)
		return
	}

	if matched {
		handleMatch(game, playerIndex, -1)
		return
	}

	handleTurnSwitch(game, playerIndex)
}

// Function to handle player quitting
func handleQuit(gameID string, playerIdx int) {
	gamesMux.Lock()
	game, exists := games[gameID]
	gamesMux.Unlock()
	if !exists {
		return
	}

	game.Mutex.Lock()
	defer game.Mutex.Unlock()

	if game.GameStatus == "game_end" {
		return
	}

	game.GameStatus = "game_end"
	winner := 1 - playerIdx
	game.Winner = winner // Set the winner
	fmt.Printf("[QUIT] %s quits | %s wins by default | Game ID: %s\n", game.Usernames[playerIdx], game.Usernames[winner], gameID)

	// Send game end event
	sendGameEndEvent(game, gameID)
}

// Function to handle flipping cards and check if they match
func handleFlip(gameID string, playerIndex int, cardIndex int) {
	gamesMux.Lock()
	game, exists := games[gameID]
	gamesMux.Unlock()
	if !exists {
		fmt.Println("[ERROR] Game not found:", gameID)
		return
	}

	game.Mutex.Lock()
	defer game.Mutex.Unlock()

	if game.GameStatus == "game_end" {
		fmt.Println("[INFO] Flip ignored, game is over")
		return
	}

	if cardIndex < 0 || cardIndex >= len(game.Cards) {
		fmt.Printf("[WARNING] %s attempted to flip an invalid card index %d | Game ID: %s\n", game.Usernames[playerIndex], cardIndex, gameID)
		return
	}

	if game.CurrentPlayer != playerIndex {
		fmt.Printf("[WARNING] %d tried to flip out of turn in Game ID: %s\n", playerIndex, gameID)
		return
	}

	if game.Paired[cardIndex] {
		fmt.Printf("[INFO] %s tried to flip an already paired card at index %d | Game ID: %s\n", game.Usernames[playerIndex], cardIndex, gameID)
		return
	}

	if game.FlippedCard == -1 {
		game.FlippedCard = cardIndex
		fmt.Printf("[FLIP] %s flipped card at index %d | Pair ID: %d\n", game.Usernames[playerIndex], cardIndex, game.PairIDs[cardIndex])
		return
	}

	if game.FlippedCard == cardIndex {
		fmt.Printf("[WARNING] %s tried to flip the same card at index %d twice | Game ID: %s\n", game.Usernames[playerIndex], cardIndex, gameID)
		return
	}

	fmt.Printf("[FLIP] %s flipped card at index %d | Pair ID: %d\n", game.Usernames[playerIndex], cardIndex, game.PairIDs[cardIndex])

	// Check if the flipped cards match
	if game.PairIDs[game.FlippedCard] == game.PairIDs[cardIndex] {
		handleMatch(game, playerIndex, cardIndex)
		game.FlippedCard = -1 // Reset flipped card after a successful match
		return
	}

	// If no match, switch turn
	handleTurnSwitch(game, playerIndex)
	game.FlippedCard = -1 // Reset flipped card after a failed match
}

// Function to handle a successful match
func handleMatch(game *Game, playerIndex int, cardIndex int) {
	game.Scores[playerIndex] += 10
	if cardIndex != -1 {
		game.Paired[game.FlippedCard] = true
		game.Paired[cardIndex] = true
	}
	fmt.Printf("[MATCH] %s matches successfully! +10 points | Round: %d\n", game.Usernames[playerIndex], game.Round)

	// Send match event
	sendMatchEvent(game, "", playerIndex)
}

// Function to handle turn switching
func handleTurnSwitch(game *Game, playerIndex int) {
	if game.GameStatus != "game_end" { // ensure the game is active before incrementing the round
		game.CurrentPlayer = 1 - playerIndex // switch to the other player
		game.Round++                         // increment the round when the turn is passed
		game.FlippedCard = -1                // reset the flipped card for the next player

		fmt.Printf("[TURN SWITCH] %s failed to match | Round: %d | Turn goes to %s\n", game.Usernames[playerIndex], game.Round, game.Usernames[game.CurrentPlayer])

		// Send turn switch event details
		sendTurnSwitchEvent(game, "")
	}
}

// Function to send game setup information
func sendGameReadyEvent(game *Game, gameID string) {
	for i, player := range game.Players {
		if player != nil {
			opponentName := game.Usernames[1-i]
			yourName := game.Usernames[i]
			event := map[string]interface{}{
				"event":        "game_ready",
				"cards":        game.Cards,
				"opponentName": opponentName,
				"yourName":     yourName,
				"timeDuration": game.Timer,
			}

			message, err := json.Marshal(event)
			if err != nil {
				fmt.Printf("[ERROR] Failed to serialize game setup event for Player %d | Error: %v\n", i, err)
				continue
			}
			player.WriteMessage(websocket.TextMessage, message)
		}
	}
}

// Function to send game state when both players are ready
func sendPlayersReadyEvent(game *Game, gameID string) {
	for i, player := range game.Players {
		if player != nil {
			event := map[string]interface{}{
				"event":      "players_ready",
				"yourScore":  game.Scores[i],
				"oppScore":   game.Scores[1-i],
				"whoseTurn":  game.Usernames[game.CurrentPlayer],
			}

			message, err := json.Marshal(event)
			if err != nil {
				fmt.Printf("[ERROR] Failed to serialize players ready event for Player %d | Error: %v\n", i, err)
				continue
			}
			player.WriteMessage(websocket.TextMessage, message)
		}
	}
}

// Function to send game state for a matched cards event
func sendMatchEvent(game *Game, gameID string, playerIndex int) {
	for i, player := range game.Players {
		if player != nil {
			event := map[string]interface{}{
				"event":      "cards_matched",
				"yourScore":  game.Scores[i],
				"oppScore":   game.Scores[1-i],
				"paired":     game.Paired,
				"whoseTurn":  game.Usernames[game.CurrentPlayer],
			}

			message, err := json.Marshal(event)
			if err != nil {
				fmt.Printf("[ERROR] Failed to serialize match event for Player %d | Error: %v\n", i, err)
				continue
			}
			player.WriteMessage(websocket.TextMessage, message)
		}
	}
}

// Function to send game state for a turn switch event
func sendTurnSwitchEvent(game *Game, gameID string) {
	for i, player := range game.Players {
		if player != nil {
			whoseTurn := game.Usernames[game.CurrentPlayer]

			event := map[string]interface{}{
				"event":      "turn_switch",
				"round":      game.Round,
				"whoseTurn":  whoseTurn,
			}

			message, err := json.Marshal(event)
			if err != nil {
				fmt.Printf("[ERROR] Failed to serialize turn switch event for %s | Error: %v\n", game.Usernames[i], err)
				continue
			}
			player.WriteMessage(websocket.TextMessage, message)
		}
	}
}

// Function to send game state for game end event
func sendGameEndEvent(game *Game, gameID string) {
	for i, player := range game.Players {
		if player != nil {
			event := map[string]interface{}{
				"event":      "game_end",
				"winner":     game.Winner,
				"scores":     game.Scores,
				"usernames":  game.Usernames,
			}

			message, err := json.Marshal(event)
			if err != nil {
				fmt.Printf("[ERROR] Failed to serialize game end event for %s | Error: %v\n", game.Usernames[i], err)
				continue
			}

			player.WriteMessage(websocket.TextMessage, message)
		}
	}
}

// Function to start the WebSocket server
func main() {
	http.HandleFunc("/ws", handleConnection)
	fmt.Println("Server started on :8082")
	http.ListenAndServe(":8082", nil)
}
