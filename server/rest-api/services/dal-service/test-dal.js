require("dotenv").config();
const mongoose = require("mongoose");

// Import Cards DAL methods
const { getCards, upsertCard } = require("./data-service/cardsService");
// Import User DAL methods
const { upsertUser, getAllUsers, updateUserJSON } = require("./data-service/userService");
// Import GameHistory DAL methods
const { upsertGameHistory, getAllGameHistories, updateGameHistoryJSON } = require("./data-service/gameHistoryService");

const MONGO_URI = process.env.MONGO_URI || "mongodb://root:example@mongo:27017/cache_game?authSource=admin";

// Connect to MongoDB before running tests
const connectDB = async () => {
  console.log("🔍 Connecting to MongoDB...");
  await mongoose.connect(MONGO_URI);
  console.log("✅ MongoDB Connected.");
};

const runTests = async () => {
  await connectDB();

  console.log("\n🃏 Running Cards DAL Tests...");

  // 🃏 Upsert a card
  const newCard = await upsertCard({
    pair_id: 24,
    pair: {
      answer: "Swift",
      question: "Often described as “Objective C without the baggage of C”"
    }
  });
  console.log("✅ Upserted Card:", newCard);

  // 🔍 Fetch all cards
  const cards = await getCards();
  console.log("📜 All Cards:", cards);

  console.log("\n👤 Running User DAL Tests...");

  // Upsert 5 users
  const usersData = [
    { username: "testUser6", password: "testPassword1", token: "testToken1", total_score: 100 },
    { username: "testUser5", password: "testPassword2", token: "testToken2", total_score: 200 },
    { username: "testUser7", password: "testPassword3", token: "testToken3", total_score: 300 },
    { username: "testUser8", password: "testPassword4", token: "testToken4", total_score: 400 },
    { username: "testUser9", password: "testPassword5", token: "testToken5", total_score: 500 }
  ];

  for (const userData of usersData) {
    const newUser = await upsertUser(userData);
    console.log("✅ Upserted User:", newUser);
  }

  // Update user JSON
  await updateUserJSON();

  // 🔍 Fetch all users
  const users = await getAllUsers();
  console.log("📜 All Users:", users);

  console.log("\n🎮 Running Game History DAL Tests...");

  // 🎮 Upsert multiple game histories
  const gameHistoriesData = [
    { gameroom_id: "room1", player_1: "testUser1", player_1_score: 250, player_2: "testUser2", player_2_score: 200 },
    { gameroom_id: "room2", player_1: "testUser3", player_1_score: 300, player_2: "testUser4", player_2_score: 250 },
    { gameroom_id: "room3", player_1: "testUser5", player_1_score: 350, player_2: "testUser1", player_2_score: 300 },
    { gameroom_id: "room4", player_1: "testUser2", player_1_score: 400, player_2: "testUser3", player_2_score: 350 },
    { gameroom_id: "room5", player_1: "testUser4", player_1_score: 450, player_2: "testUser5", player_2_score: 400 }
  ];

  for (const gameHistoryData of gameHistoriesData) {
    const newGameHistory = await upsertGameHistory(gameHistoryData);
    console.log("✅ Upserted Game History:", newGameHistory);
  }

  // Update game history JSON
  await updateGameHistoryJSON();

  // 🔍 Fetch all game histories
  const gameHistories = await getAllGameHistories();
  console.log("📜 All Game Histories:", gameHistories);

  // Close connection after tests
  mongoose.connection.close();
  console.log("🔌 MongoDB Connection Closed.");
};

// Run the tests
runTests();