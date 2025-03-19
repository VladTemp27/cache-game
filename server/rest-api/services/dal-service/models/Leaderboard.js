const mongoose = require("mongoose");

const LeaderboardFormat = new mongoose.Schema({
    user_id: { type: mongoose.Schema.Types.ObjectId, ref: "User", required: true },
    total_score: { type: Number, required: true }
});

module.exports = mongoose.model("Leaderboard", LeaderboardFormat);