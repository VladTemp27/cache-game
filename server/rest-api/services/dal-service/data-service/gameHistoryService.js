const fs = require("fs");
const path = require("path");
const GameHistory = require("../models/gameHistory");

const gameHistorySeedPath = path.join(__dirname, "../data/game_history.json");

const updateGameHistoryJSON = async () => {
    const allGameHistories = await GameHistory.find();
    fs.writeFileSync(gameHistorySeedPath, JSON.stringify(allGameHistories, null, 2));
};

const upsertGameHistory = async (gameHistoryData) => {
    const { gameroom_id } = gameHistoryData;
    const result = await GameHistory.findOneAndUpdate(
        { gameroom_id },
        gameHistoryData,
        { upsert: true, new: true, setDefaultsOnInsert: true }
    );
    await updateGameHistoryJSON();
    return result;
};

const getAllGameHistories = async () => {
    return await GameHistory.find();
};

const getGameHistoryById = async (id) => {
    return await GameHistory.findById(id).exec();
};

module.exports = { upsertGameHistory, getAllGameHistories, getGameHistoryById };