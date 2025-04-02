const express = require('express');
const router = express.Router();
const { getAllGameHistories, getGameHistoryById, upsertGameHistory, updateGameHistoryJSON } = require('../data-service/gameHistoryService');
const { upsertUser } = require('../data-service/userService');

// Get all game histories
router.get('/getAllGameHistories', async (req, res) => {
    try {
        const gameHistories = await getAllGameHistories();
        res.json(gameHistories);
    } catch (err) {
        res.status(500).json({ message: err.message });
    }
});

// Get a specific game history by ID
router.get('/getGameHistoryById/:id', getGameHistory, (req, res) => {
    res.json(res.gameHistory);
});

// Create a new game history
router.post('/upsertGameHistory', async (req, res) => {
    const gameHistoryData = {
        gameroom_id: req.body.gameroom_id,
        difficulty: req.body.difficulty,
        player_1: req.body.player_1,
        player_1_score: req.body.player_1_score,
        player_2: req.body.player_2,
        player_2_score: req.body.player_2_score
    };
    
    try {
        const newGameHistory = await upsertGameHistory(gameHistoryData);
        await updateGameHistoryJSON();

        if (gameHistoryData.player_1){            
            const player_1 = {
                username: gameHistoryData.player_1,
                score: gameHistoryData.player_1_score
            }
            await upsertUser(player_1)
        }

        if (gameHistoryData.player_2){
            const player_2 = {
                username: gameHistoryData.player_2,
                score: gameHistoryData.player_2_score
            }
            await upsertUser(player_2)
        }

        res.status(201).json(newGameHistory);
    } catch (err) {
        res.status(400).json({ message: err.message });
    }
});

// Middleware to get game history by ID
async function getGameHistory(req, res, next) {
    let gameHistory;
    try {
        gameHistory = await getGameHistoryById(req.params.id);
        if (gameHistory == null) {
            return res.status(404).json({ message: 'Cannot find game history' });
        }
    } catch (err) {
        return res.status(500).json({ message: err.message });
    }

    res.gameHistory = gameHistory;
    next();
}

module.exports = router;