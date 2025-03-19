require("dotenv").config();
const mongoose = require("mongoose");
const fs = require("fs");
const path = require("path");
const connectDB = require("../config/db"); 
const {
    upsertUser,
    getAllUsers,
} = require("../data-service/userService"); 
const {
    upsertLeaderboard,
    getAllLeaderboardEntries,
} = require("../data-service/leaderboardService"); 
const {
    upsertCard,
    getAllCards,
} = require("../data-service/cardsService");

const syncDatabase = async () => {
    await connectDB();

    // Read seed files
    const userSeedPath = path.join(__dirname, "../data/user.json");
    const leaderboardSeedPath = path.join(__dirname, "../data/leaderboard.json");
    const cardsSeedPath = path.join(__dirname, "../data/cards.json");

    const userSeedData = JSON.parse(fs.readFileSync(userSeedPath, "utf-8"));
    const leaderboardSeedData = JSON.parse(fs.readFileSync(leaderboardSeedPath, "utf-8"));
    const cardsSeedData = JSON.parse(fs.readFileSync(cardsSeedPath, "utf-8"));

    // Users Sync
    console.log("\n Syncing Users...");
    for (const userData of userSeedData) {
        await upsertUser(userData);
        console.log(`✅ Synced user: ${userData.username}`);
    }

    // Leaderboard Sync
    console.log("\n Syncing Leaderboard...");
    for (const leaderboardData of leaderboardSeedData) {
        await upsertLeaderboard(leaderboardData);
        console.log(`🏆 Synced leaderboard entry for user: ${leaderboardData.user_id}`);
    }

    // Cards Sync
    console.log("\n Syncing Cards...");
    for (const cardData of cardsSeedData) {
        await upsertCard(cardData);
        console.log(`🃏 Synced card: ${cardData.card_name}`);
    }

    mongoose.connection.close();
    console.log("✅ Database sync complete.");
};

// Run sync
syncDatabase();