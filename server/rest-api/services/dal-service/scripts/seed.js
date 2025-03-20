require("dotenv").config();
const mongoose = require("mongoose");
const fs = require("fs");
const path = require("path");
const connectDB = require("../config/db");

// Import models
const Cards = require("../models/Cards");
const Users = require("../models/User");
const GameHistory = require("../models/gameHistory");

// Import seed paths
const cardsSeedPath = path.join(__dirname, "../data/cards.json");
const usersSeedPath = path.join(__dirname, "../data/user.json");
const gameHistorySeedPath = path.join(__dirname, "../data/game_history.json");

// Seed cards data
const seedCards = async () => {
  console.log("Seeding Cards Data...");
 
  let cardsSeedData;
  try {
    cardsSeedData = JSON.parse(fs.readFileSync(cardsSeedPath, "utf8"));
    if (!Array.isArray(cardsSeedData)) {
      throw new Error("Cards seed data is not an array");
    }
  } catch (err) {
    console.error("Error reading cards seed data:", err);
    cardsSeedData = [];
  }

  for (const cardData of cardsSeedData) {
    try {
      await Cards.findOneAndUpdate(
        { pair_id: cardData.pair_id },
        cardData,
        { upsert: true, new: true, setDefaultsOnInsert: true }
      );
    } catch (err) {
      console.error("Error seeding card data:", err);
    }
  }

  console.log("Cards Data Seeded");
};

// Seed users data
const seedUsers = async () => {
  console.log("Seeding Users Data...");

  let usersSeedData;
  try {
    usersSeedData = JSON.parse(fs.readFileSync(usersSeedPath, "utf8"));
    if (!Array.isArray(usersSeedData)) {
      throw new Error("Users seed data is not an array");
    }
  } catch (err) {
    console.error("Error reading users seed data:", err);
    usersSeedData = [];
  }

  for (const userData of usersSeedData) {
    try {
      await Users.findOneAndUpdate(
        { username: userData.username },
        userData,
        { upsert: true, new: true, setDefaultsOnInsert: true }
      );
    } catch (err) {
      console.error("Error seeding user data:", err);
    }
  }

  console.log("Users Data Seeded");
};

// Seed game history data
const seedGameHistory = async () => {
  console.log("Seeding Game History Data...");

  let gameHistorySeedData;
  try {
    gameHistorySeedData = JSON.parse(fs.readFileSync(gameHistorySeedPath, "utf8"));
    if (!Array.isArray(gameHistorySeedData)) {
      throw new Error("Game history seed data is not an array");
    }
  } catch (err) {
    console.error("Error reading game history seed data:", err);
    gameHistorySeedData = [];
  }

  for (const gameHistoryData of gameHistorySeedData) {
    try {
      await GameHistory.findOneAndUpdate(
        { gameroom_id: gameHistoryData.gameroom_id },
        gameHistoryData,
        { upsert: true, new: true, setDefaultsOnInsert: true }
      );
    } catch (err) {
      console.error("Error seeding game history data:", err);
    }
  }

  console.log("Game History Data Seeded");
};

// Run the seeders
const seedAll = async () => {
  await seedCards();
  await seedUsers();
  await seedGameHistory();
};

module.exports = seedAll;