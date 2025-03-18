const Leaderboard = require("../models/Leaderboard");

// Create or update leaderboard entry
const leaderboardSchema = new mongoose.Schema({
    user_id: { type: String, required: true, unique: true },
    score: { type: Number, required: true },
});


// Delete leaderboard entries not in seed JSON
const deleteMissingLeaderboardEntries = async (validIds) => {
    return await Leaderboard.deleteMany({ _id: { $nin: validIds } });
};

// Get all leaderboard entries
const getAllLeaderboardEntries = async () => {
    return await Leaderboard.find().populate("user_id", "username total_score");
};

module.exports = { upsertLeaderboard, deleteMissingLeaderboardEntries, getAllLeaderboardEntries };