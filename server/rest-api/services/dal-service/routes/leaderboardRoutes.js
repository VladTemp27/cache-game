const express = require("express");
const { 
    addUserToLeaderboard, 
    getLeaderboard, 
    getLeaderboardEntry, 
    updateLeaderboardEntry, 
    removeUserFromLeaderboard 
} = require("../data-service/leaderboardService");

const router = express.Router();

// ✅ Add a user to the leaderboard
router.post("/addUserLB", async (req, res) => {
    try {
        const newEntry = await addUserToLeaderboard(req.body.user_id);
        res.status(201).json(newEntry);
    } catch (error) {
        res.status(400).json({ error: error.message });
    }
});

// ✅ Get the full leaderboard
router.get("/leaderboards", async (req, res) => {
    try {
        const leaderboard = await getLeaderboard();
        res.json(leaderboard);
    } catch (error) {
        res.status(500).json({ error: error.message });
    }
});

// ✅ Get a specific leaderboard entry by ID
router.get("/:id", async (req, res) => {
    try {
        const entry = await getLeaderboardEntry(req.params.id);
        if (!entry) return res.status(404).json({ message: "Leaderboard entry not found" });
        res.json(entry);
    } catch (error) {
        res.status(500).json({ error: error.message });
    }
});

// ✅ Update a leaderboard entry
router.put("/:id", async (req, res) => {
    try {
        const updatedEntry = await updateLeaderboardEntry(req.params.id, req.body);
        if (!updatedEntry) return res.status(404).json({ message: "Leaderboard entry not found" });
        res.json(updatedEntry);
    } catch (error) {
        res.status(400).json({ error: error.message });
    }
});

// ✅ Remove a user from the leaderboard
router.delete("/:id", async (req, res) => {
    try {
        const deletedEntry = await removeUserFromLeaderboard(req.params.id);
        if (!deletedEntry) return res.status(404).json({ message: "Leaderboard entry not found" });
        res.json({ message: "Leaderboard entry removed successfully" });
    } catch (error) {
        res.status(500).json({ error: error.message });
    }
});

module.exports = router;
