package main

import (
	"time"

	"go.mongodb.org/mongo-driver/bson/primitive"
)

// QueuePlayer represents a player in the matchmaking queue
type QueuePlayer struct {
    ID       primitive.ObjectID `bson:"_id,omitempty" json:"id,omitempty"`
    Username string             `bson:"username" json:"username"`
    Token    string             `bson:"token" json:"token"`
    Score    int                `bson:"score" json:"score"`
    Cluster  string                `bson:"cluster" json:"cluster"`
    JoinedAt time.Time          `bson:"joinedAt,omitempty" json:"joinedAt,omitempty"`
}