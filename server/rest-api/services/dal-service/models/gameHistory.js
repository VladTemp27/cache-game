const mongoose = require("mongoose");

const GameHistorySchema = new mongoose.Schema({
    gameroom_id: { type: String, required: true },
    player_1: { type: String, required: true },
    player_1_score: { type: Number, required: true },
    player_2: { type: String, required: true },
    player_2_score: { type: Number, required: true }
}, {collection: "game_history"}); 

module.exports = mongoose.models.GameHistory || mongoose.model("GameHistory", GameHistorySchema);