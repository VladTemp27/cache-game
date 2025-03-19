const User = require("../models/User");

// ✅ Upsert user
const upsertUser = async (userData) => {
    const { user_id } = userData;
    return await User.findOneAndUpdate(
        { user_id },
        userData,
        { upsert: true, new: true, setDefaultsOnInsert: true }
    );
};

// check for an existing user
const verifyUser = async (username) => {
    return await User.findOne({ username });
};

// ✅ Get all users
const getAllUsers = async () => {
    return await User.find();
};

// ✅ Update or Add Game History Entry
const updateGameHistory = async (userId, gameData) => {
    return await User.findOneAndUpdate(
        { _id: userId, "game_history.gameroom_id": gameData.gameroom_id },
        { $set: { "game_history.$": gameData } }, // Update existing entry
        { new: true }
    ) || await User.findByIdAndUpdate(
        userId,
        { $push: { game_history: gameData } }, // Add new entry if not found
        { new: true }
    );
};

// ✅ Delete a specific game history entry
const deleteGameHistory = async (userId, gameroomId) => {
    return await User.findByIdAndUpdate(
        userId,
        { $pull: { game_history: { gameroom_id: gameroomId } } }, // Remove specific entry
        { new: true }
    );
};

module.exports = { upsertUser, verifyUser, getAllUsers, updateGameHistory, deleteGameHistory };