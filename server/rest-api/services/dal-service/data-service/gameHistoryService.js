const fs = require("fs");
const path = require("path");
const GameHistory = require("../models/gameHistory");
const { verifyUser, upsertUser } = require("./userService");

const gameHistorySeedPath = path.join(__dirname, "../data/game_history.json");

const updateGameHistoryJSON = async () => {
    const allGameHistories = await GameHistory.find();
    fs.writeFileSync(gameHistorySeedPath, JSON.stringify(allGameHistories, null, 2));
};

const updateUserScores = async (gameHistoryData) => {
    if (gameHistoryData.player_1) {
        const existingUser1 = await verifyUser(gameHistoryData.player_1);
        
        if (existingUser1) {
            const currentTotal1 = existingUser1.total_score || 0;
            const player1Data = {
                username: gameHistoryData.player_1,
                total_score: currentTotal1 + gameHistoryData.player_1_score,
                password: existingUser1.password,
                token: existingUser1.token
            };
            
            await upsertUser(player1Data);
        }
    }

    if (gameHistoryData.player_2) {
        const existingUser2 = await verifyUser(gameHistoryData.player_2);
        
        if (existingUser2) {
            const currentTotal2 = existingUser2.total_score || 0;
            const player2Data = {
                username: gameHistoryData.player_2,
                total_score: currentTotal2 + gameHistoryData.player_2_score,
                password: existingUser2.password,
                token: existingUser2.token
            };
            
            await upsertUser(player2Data);
        }
    }
};

const upsertGameHistory = async (gameHistoryData) => {
    const { gameroom_id } = gameHistoryData;
    const result = await GameHistory.findOneAndUpdate(
        { gameroom_id },
        gameHistoryData,
        { upsert: true, new: true, setDefaultsOnInsert: true }
    );
    await updateGameHistoryJSON();
    await updateUserScores(gameHistoryData);
    return result;
};

const getAllGameHistories = async () => {
    return await GameHistory.find();
};

const getGameHistoryById = async (id) => {
    return await GameHistory.findById(id).exec();
};

module.exports = { upsertGameHistory, getAllGameHistories, getGameHistoryById, updateGameHistoryJSON };