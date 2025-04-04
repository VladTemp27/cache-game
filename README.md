# Cache Game Project

## Overview
Cache Game is a multiplayer card game application with a client-server architecture. The project consists of a Java client application for the game interface and a microservices backend infrastructure handling game logic, matchmaking, and user management.

## Project Structure

- **Client Application**: Java-based frontend with JavaFX for UI components
- **Backend Services**:
  - REST API service (Node.js)
  - Game service (Go)
  - Matchmaking service (Go)

## Features

- User authentication and session management
- Matchmaking system for pairing players
- Real-time card game functionality
- Game history tracking
- Responsive and intuitive user interface

## Requirements

### Client Application
- Java 21
- Maven

### Server Components
- Docker and Docker Compose
- Go 1.16+ (for game and matchmaking services)
- Node.js 14+ (for REST API)

## Setup & Installation

### 1. Clone the repository
```bash
git clone https://github.com/VladTemp27/cache-game.git
cd cache-game
```

### 2. Start the backend services
```bash
docker-compose up
```

### 3.1 Build and run the client application
```bash
cd app
./mvnw clean javafx:run
```
### 3.2 Build an executable package
See [Game Distribution Guide](app/doc/dist.md)
## Development

### Client Application
The client is structured following the MVC pattern:
- Models handle data and business logic
- Views are defined in FXML in the resources directory
- Controllers manage user interactions

### Backend Services

#### REST API (Node.js)
- Handles authentication and data persistence
- Provides endpoints for user management and game history
- Uses JSON for data storage

#### Game Service (Go)
- Manages active game sessions
- Implements game logic and rules
- Communicates with clients via WebSockets

#### Matchmaking Service (Go)
- Pairs players for games based on availability and criteria
- Queue management for waiting players

## Configuration
Nginx is configured as a reverse proxy to route requests to appropriate services. Configuration can be modified in [`nginx.conf`](nginx.conf).
Environment variables for services can be found in respective `.env` files.
