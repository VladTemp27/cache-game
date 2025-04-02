const express = require('express');
const router = express.Router();
const { getAllGameHistories, getGameHistoryById, upsertGameHistory, updateGameHistoryJSON } = require('../data-service/gameHistoryService');
const { upsertUser, verifyUser } = require('../data-service/userService');

router.get('/getAllGameHistories', async (req, res) => {
    try {
        const gameHistories = await getAllGameHistories();
        res.json(gameHistories);
    } catch (err) {
        res.status(500).json({ message: err.message });
    }
});

router.get('/getGameHistoryById/:id', getGameHistory, (req, res) => {
    res.json(res.gameHistory);
});

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
        
        res.status(201).json(newGameHistory);
    } catch (err) {
        res.status(400).json({ message: err.message });
    }
});

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