const mongoose = require("mongoose");

const UserFormat = new mongoose.Schema({
    username: { type: String, required: true, unique: true },
    password: { type: String, required: true }, // Hashed password
    token: { type: String },
    total_score: { type: Number, default: 0 },
    game_history: [{
        gameroom_id: String,
        difficulty: String,
        player_1: String,
        player_1_score: Number,
        player_2: String,
        player_2_score: Number
    }]
});

module.exports = mongoose.model("User", UserFormat);
