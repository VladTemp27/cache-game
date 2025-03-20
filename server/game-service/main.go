package main

import (
	"encoding/json"
	"fmt"
	"math/rand"
	"net/http"
	"sync"
	"time"

	"github.com/gorilla/websocket"
)

// This is a Game struct which represents a single game instance
type Game struct {
	Mutex       sync.Mutex
	Players     []*websocket.Conn // tracks the players in a game
	Scores      []int             // tracks the scores of both players
	Turn        int
	Timer       int    // tracks the time left in a game
	Difficulty  string // tracks the difficulty of a game
	GameOver    bool   // tracks if a game is over
	Active      bool   // tracks if a game is active
	Round       int    // tracks the number of rounds in a game
	LoopRunning bool   // indicates if the game loop is running
}

// This section is for the global variables
var (
	games    = make(map[string]*Game) // store active games
	gamesMux sync.Mutex
	upgrader = websocket.Upgrader{
		CheckOrigin: func(r *http.Request) bool { return true },
	}
)

// This function is responsible for starting the game loop
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
	gamesMux.Unlock()

	rand.Seed(time.Now().UnixNano())
	game.Turn = rand.Intn(2) // this is for randomly selecting the starting player
	game.Round = 1           // the round will start in 1

	// This section is for the difficulty levels
	durations := map[string]int{"easy": 180, "medium": 420, "hard": 600}
	game.Timer = durations[game.Difficulty]
	game.Active = true

	fmt.Printf("[GAME START] Game ID: %s | Difficulty: %s | Timer: %d seconds | Player %d starts (Round %d)\n",
		gameID, game.Difficulty, game.Timer, game.Turn, game.Round)

	// This function is responsible for starting the game loop in a new goroutine
	go func() {
		ticker := time.NewTicker(1 * time.Second)
		defer ticker.Stop()

		for game.Timer > 0 {
			<-ticker.C

			game.Mutex.Lock()
			if game.GameOver {
				game.Mutex.Unlock()
				fmt.Printf("[GAME OVER] Game ID: %s\n", gameID)
				return
			}
			game.Timer--
			fmt.Printf("[TIMER] Game ID: %s | Time Left: %d seconds | Round: %d | Scores: [%d, %d]\n",
				gameID, game.Timer, game.Round, game.Scores[0], game.Scores[1])
			game.broadcastState()
			game.Mutex.Unlock()
		}

		// This section means the game is over
		game.Mutex.Lock()
		game.GameOver = true
		game.LoopRunning = false
		game.broadcastState()
		game.Mutex.Unlock()

		// This section determines the winner of the game, and tie if there are no winners
		winner := -1
		if game.Scores[0] > game.Scores[1] {
			winner = 0
		} else if game.Scores[1] > game.Scores[0] {
			winner = 1
		}

		// This section broadcasts the game over message with the winner
		game.Mutex.Lock()
		state := map[string]interface{}{
			"scores":   game.Scores,
			"turn":     game.Turn,
			"timer":    game.Timer,
			"gameOver": game.GameOver,
			"round":    game.Round,
			"winner":   winner,
		}
		message, _ := json.Marshal(state)
		for i, player := range game.Players {
			if player != nil {
				err := player.WriteMessage(websocket.TextMessage, message)
				if err != nil {
					fmt.Printf("[ERROR] Failed to send state to Player %d | Error: %v\n", i, err)
				}
			}
		}
		game.Mutex.Unlock()

		if winner != -1 {
			fmt.Printf("[TIME UP] Game ID: %s | Game Over! Player %d wins with %d points\n", gameID, winner, game.Scores[winner])
		} else {
			fmt.Printf("[TIME UP] Game ID: %s | Game Over! It's a tie with %d points each\n", gameID, game.Scores[0])
		}
	}()
}

// This section broadcasts the game state to both players
func (g *Game) broadcastState() {
	state := map[string]interface{}{
		"scores":   g.Scores,
		"turn":     g.Turn,
		"timer":    g.Timer,
		"gameOver": g.GameOver,
		"round":    g.Round,
	}

	message, _ := json.Marshal(state)
	for i, player := range g.Players {
		if player != nil {
			err := player.WriteMessage(websocket.TextMessage, message)
			if err != nil {
				fmt.Printf("[ERROR] Failed to send state to Player %d | Error: %v\n", i, err)
			}
		}
	}
}

// This section handles a player's move
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

	if game.GameOver {
		fmt.Println("[INFO] Move ignored, game is over")
		return
	}

	if game.Turn != playerIndex {
		fmt.Printf("[WARNING] Player %d tried to move out of turn in Game ID: %s\n", playerIndex, gameID)
		return
	}

	if matched {
		scoreIncrement := map[string]int{"easy": 10, "medium": 16, "hard": 22}[game.Difficulty]
		game.Scores[playerIndex] += scoreIncrement
		fmt.Printf("[MATCH] Player %d matched! +%d points | Game ID: %s | Round: %d\n", playerIndex, scoreIncrement, gameID, game.Round)
	} else {
		game.Turn = 1 - playerIndex
		game.Round++ // the game round increments when the turn is passed
		fmt.Printf("[TURN SWITCH] Player %d failed to match | Game ID: %s | Round: %d | Turn goes to Player %d\n",
			playerIndex, gameID, game.Round, game.Turn)
	}

	game.broadcastState()
}

// This function handles new WebSocket connections
func handleConnection(w http.ResponseWriter, r *http.Request) {
	conn, err := upgrader.Upgrade(w, r, nil)
	if err != nil {
		fmt.Println("[ERROR] WebSocket upgrade failed:", err)
		return
	}

	gameID := r.URL.Query().Get("gameID")

	gamesMux.Lock()
	game, exists := games[gameID]
	if !exists {
		games[gameID] = &Game{
			Difficulty: "easy",
			Players:    make([]*websocket.Conn, 2),
			Scores:     make([]int, 2),
		}
		game = games[gameID]
		fmt.Printf("[NEW GAME] Created game %s\n", gameID)
	}
	gamesMux.Unlock()

	game.Mutex.Lock()
	defer game.Mutex.Unlock()

	// This section is responsible in finding the first available slot for a player
	var playerIdx int = -1
	for i := 0; i < 2; i++ {
		if game.Players[i] == nil {
			playerIdx = i
			break
		}
	}

	if playerIdx == -1 {
		fmt.Println("[ERROR] Game already has 2 players, rejecting connection...")
		conn.Close()
		return
	}

	game.Players[playerIdx] = conn
	fmt.Printf("[CONNECTED] Player %d joined Game ID: %s\n", playerIdx, gameID)

	// This is responsible in starting a game when both players are connected
	if game.Players[0] != nil && game.Players[1] != nil {
		startGame(gameID)
	}

	// This keep-alive pings and disconnection detection prevents the websocket connection from closing
	go func() {
		pingTicker := time.NewTicker(5 * time.Second)
		defer pingTicker.Stop()

		for {
			select {
			case <-pingTicker.C:
				game.Mutex.Lock()
				if game.GameOver {
					game.Mutex.Unlock()
					return
				}
				if err := conn.WriteMessage(websocket.PingMessage, nil); err != nil {
					fmt.Printf("[DISCONNECTED] Player %d lost connection | Game ID: %s\n", playerIdx, gameID)
					game.Players[playerIdx] = nil
					conn.Close()
					game.Mutex.Unlock()
					handleDisconnection(gameID, playerIdx)
					return
				}
				game.Mutex.Unlock()
			}
		}
	}()

	// This function listens for messages from client
	go func() {
		defer conn.Close()
		for {
			_, message, err := conn.ReadMessage()
			if err != nil {
				fmt.Printf("[DISCONNECTED] Player %d closed connection | Game ID: %s\n", playerIdx, gameID)
				game.Mutex.Lock()
				game.Players[playerIdx] = nil
				game.Mutex.Unlock()
				handleDisconnection(gameID, playerIdx)
				return
			}

			var payload struct {
				Action  string `json:"action"`
				Matched bool   `json:"matched"`
			}
			if err := json.Unmarshal(message, &payload); err == nil {
				switch payload.Action {
				case "move":
					handleMove(gameID, playerIdx, payload.Matched)
				case "quit":
					handleQuit(gameID, playerIdx)
				}
			}
		}
	}()
}

// This function handles player quitting
func handleQuit(gameID string, playerIdx int) {
	gamesMux.Lock()
	game, exists := games[gameID]
	gamesMux.Unlock()
	if !exists {
		return
	}

	game.Mutex.Lock()
	defer game.Mutex.Unlock()

	if game.GameOver {
		return
	}

	game.GameOver = true
	game.broadcastState()
	fmt.Printf("[QUIT] Player %d quit | Player %d wins by default | Game ID: %s\n", playerIdx, 1-playerIdx, gameID)
}

// This function handles player disconnection
func handleDisconnection(gameID string, playerIdx int) {
	gamesMux.Lock()
	game, exists := games[gameID]
	gamesMux.Unlock()
	if !exists {
		return
	}

	game.Mutex.Lock()
	defer game.Mutex.Unlock()

	if game.GameOver {
		return
	}

	fmt.Printf("[WAITING] Player %d disconnected | Waiting 10s before declaring other player winner | Game ID: %s\n",
		playerIdx, gameID)

	go func() {
		time.Sleep(10 * time.Second)
		game.Mutex.Lock()
		if game.Players[playerIdx] == nil {
			game.GameOver = true
			game.broadcastState()
			fmt.Printf("[WIN] Player %d wins by default | Game ID: %s\n", 1-playerIdx, gameID)
		}
		game.Mutex.Unlock()
	}()
}

// This function is responsible for starting the WebSocket server
func main() {
	http.HandleFunc("/ws", handleConnection)
	fmt.Println("Server started on :8080")
	http.ListenAndServe(":8080", nil)
}
