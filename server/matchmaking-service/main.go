package main

import (
    "fmt"
    "log"
    "net/http"
    "sync"

    "github.com/gorilla/websocket"
)

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
    fmt.Println("Matchmaking service started on port 8080")

    manager := NewClientManager()
    clientCounter := 0

    // HTTP endpoint for info
    http.HandleFunc("/", func(w http.ResponseWriter, r *http.Request) {
        fmt.Fprintf(w, "Matchmaking service - Connect via WebSocket at /ws")
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
    log.Fatal(http.ListenAndServe(":8080", nil))
}

func handleClient(client *Client, manager *ClientManager) {
    // Ensure we remove the client when the function exits
    defer func() {
        client.conn.Close()
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

        // Log the received message
        log.Printf("Received message from %s: %s", client.id, string(message))

        // Echo the message back to the client
        if err := client.conn.WriteMessage(messageType, message); err != nil {
            log.Printf("Error writing message: %v", err)
            break
        }
    }
}