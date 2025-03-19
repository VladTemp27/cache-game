const fs = require("fs");
const path = require("path");
const Leaderboard = require("../models/Leaderboard");
const User = require("../models/User");

const leaderboardSeedPath = path.join(__dirname, "../data/leaderboard.json");

const updateLeaderboardJSON = async () => {
    const allLeaderboardEntries = await Leaderboard.find().populate("user_id", "username total_score");
    fs.writeFileSync(leaderboardSeedPath, JSON.stringify(allLeaderboardEntries, null, 2));
};

const upsertLeaderboard = async () => {
    // Fetch all users
    const users = await User.find();

    // Clear the leaderboard collection
    await Leaderboard.deleteMany({});

    // Insert users into the leaderboard collection with their total scores
    for (const user of users) {
        await Leaderboard.create({
            user_id: user._id,
            total_score: user.total_score
        });
    }

    // Update the leaderboard JSON file
    await updateLeaderboardJSON();
};

const getAllLeaderboardEntries = async () => {
    return await Leaderboard.find()
        .populate("user_id", "username total_score")
        .sort({ total_score: -1 }); // Sort by total_score in descending order
};

module.exports = { upsertLeaderboard, getAllLeaderboardEntries };