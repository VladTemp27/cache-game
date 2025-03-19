package main

import (
    "context"
    "fmt"
    "log"
    "os"
    "time"

    "go.mongodb.org/mongo-driver/bson"
    "go.mongodb.org/mongo-driver/bson/primitive"
    "go.mongodb.org/mongo-driver/mongo"
    "go.mongodb.org/mongo-driver/mongo/options"
)

// Fixed typo in environment variable name
func connectToMongoDB() (*mongo.Client, error) {
    uri := os.Getenv("MONGO_URI")
    clientOptions := options.Client().ApplyURI(uri)
    client, err := mongo.Connect(context.Background(), clientOptions)
    if err != nil {
        log.Fatalf("Error connecting to MongoDB: %v\n", err)
    }
    err = client.Ping(context.Background(), nil)
    if err != nil {
        log.Fatalf("Error pinging MongoDB: %v\n", err)
    }
    fmt.Println("Connected to MongoDB")
    return client, nil
}

// AddPlayerToQueue adds a player to the matchmaking queue
func AddPlayerToQueue(player *QueuePlayer) error {
    client, err := connectToMongoDB()
    if err != nil {
        return fmt.Errorf("failed to connect to MongoDB: %v", err)
    }
    defer client.Disconnect(context.Background())

    collection := client.Database("matchmaking").Collection("queue")
    
    // Check if player with the same username already exists in the queue
    filter := bson.M{"username": player.Username}
    var existingPlayer QueuePlayer
    err = collection.FindOne(context.Background(), filter).Decode(&existingPlayer)
    
    if err == nil {
        // Player already exists in queue, update their record
        update := bson.M{
            "$set": bson.M{
                "token":    player.Token,
                "score":    player.Score,
                "cluster":  player.Cluster,
                "joinedAt": time.Now(),
            },
        }
        _, err = collection.UpdateOne(context.Background(), filter, update)
        if err != nil {
            return fmt.Errorf("failed to update player in queue: %v", err)
        }
        return nil
    }
    
    // Player doesn't exist, insert a new record
    _, err = collection.InsertOne(context.Background(), player)
    if err != nil {
        return fmt.Errorf("failed to add player to queue: %v", err)
    }
    
    return nil
}

// RemovePlayerFromQueue removes a player from the matchmaking queue
func RemovePlayerFromQueue(username string, token string) error {
    client, err := connectToMongoDB()
    if err != nil {
        return fmt.Errorf("failed to connect to MongoDB: %v", err)
    }
    defer client.Disconnect(context.Background())

    collection := client.Database("matchmaking").Collection("queue")
    
    // Remove player with matching username and token
    filter := bson.M{"username": username, "token": token}
    result, err := collection.DeleteOne(context.Background(), filter)
    
    if err != nil {
        return fmt.Errorf("failed to remove player from queue: %v", err)
    }
    
    if result.DeletedCount == 0 {
        return fmt.Errorf("player not found in queue")
    }
    
    return nil
}

// GetPlayerFromQueue gets a player from the queue by username
func GetPlayerFromQueue(username string) (*QueuePlayer, error) {
    client, err := connectToMongoDB()
    if err != nil {
        return nil, fmt.Errorf("failed to connect to MongoDB: %v", err)
    }
    defer client.Disconnect(context.Background())

    collection := client.Database("matchmaking").Collection("queue")
    
    filter := bson.M{"username": username}
    var player QueuePlayer
    err = collection.FindOne(context.Background(), filter).Decode(&player)
    
    if err != nil {
        if err == mongo.ErrNoDocuments {
            return nil, fmt.Errorf("player not found in queue")
        }
        return nil, fmt.Errorf("error finding player: %v", err)
    }
    
    return &player, nil
}