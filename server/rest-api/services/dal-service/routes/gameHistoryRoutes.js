const express = require('express');
const router = express.Router();
const GameHistory = require('../models/GameHistory');

// Get all game histories
router.get('/getAllGameHistories', async (req, res) => {
    try {
        const gameHistories = await GameHistory.find();
        res.json(gameHistory);
    } catch (err) {
        res.status(500).json({ message: err.message });
    }
});

// Get a specific game history by ID
router.get('/getGameHistoryById/:id', getGameHistory, (req, res) => {
    res.json(res.gameHistory);
});

// Create a new game history
router.post('/createGameHistory', async (req, res) => {
    const gameHistory = new GameHistory({
        gameroom_id: req.body.gameroom_id,
        difficulty: req.body.difficulty,
        player_1: req.body.player_1,
        player_1_score: req.body.player_1_score,
        player_2: req.body.player_2,
        player_2_score: req.body.player_2_score
    });

    try {
        const newGameHistory = await gameHistory.save();
        res.status(201).json(newGameHistory);
    } catch (err) {
        res.status(400).json({ message: err.message });
    }
});

// Update a game history by ID
router.put('/updateGameHistory/:id', getGameHistory, async (req, res) => {
    if (req.body.gameroom_id != null) {
        res.gameHistory.gameroom_id = req.body.gameroom_id;
    }
    if (req.body.difficulty != null) {
        res.gameHistory.difficulty = req.body.difficulty;
    }
    if (req.body.player_1 != null) {
        res.gameHistory.player_1 = req.body.player_1;
    }
    if (req.body.player_1_score != null) {
        res.gameHistory.player_1_score = req.body.player_1_score;
    }
    if (req.body.player_2 != null) {
        res.gameHistory.player_2 = req.body.player_2;
    }
    if (req.body.player_2_score != null) {
        res.gameHistory.player_2_score = req.body.player_2_score;
    }

    try {
        const updatedGameHistory = await res.gameHistory.save();
        res.json(updatedGameHistory);
    } catch (err) {
        res.status(400).json({ message: err.message });
    }
});

// Delete a game history by ID
router.delete('/deleteGameHistory/:id', getGameHistory, async (req, res) => {
    try {
        await res.gameHistory.remove();
        res.json({ message: 'Deleted Game History' });
    } catch (err) {
        res.status(500).json({ message: err.message });
    }
});

// Middleware to get game history by ID
async function getGameHistory(req, res, next) {
    let gameHistory;
    try {
        gameHistory = await GameHistory.findById(req.params.id);
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