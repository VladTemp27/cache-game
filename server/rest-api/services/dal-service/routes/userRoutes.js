const express = require("express");
const { 
    createUser, 
    getUserById, 
    getAllUsers, 
    updateUserScore, 
    deleteUser, 
    updateGameHistory, 
    deleteGameHistory 
} = require("../data-service/userService");

const router = express.Router();

// ✅ Create a new user
router.post("/createUser", async (req, res) => {
    try {
        const newUser = await createUser(req.body);
        res.status(201).json(newUser);
    } catch (error) {
        res.status(400).json({ error: error.message });
    }
});

// ✅ Get all users
router.get("/users", async (req, res) => {
    try {
        const users = await getAllUsers();
        res.json(users);
    } catch (error) {
        res.status(500).json({ error: error.message });
    }
});

// ✅ Get a specific user by ID
router.get("/:id", async (req, res) => {
    try {
        const user = await getUserById(req.params.id);
        if (!user) return res.status(404).json({ message: "User not found" });
        res.json(user);
    } catch (error) {
        res.status(500).json({ error: error.message });
    }
});

// ✅ Update user total score
router.put("/:id", async (req, res) => {
    try {
        const updatedUser = await updateUserScore(req.params.id, req.body.total_score);
        if (!updatedUser) return res.status(404).json({ message: "User not found" });
        res.json(updatedUser);
    } catch (error) {
        res.status(400).json({ error: error.message });
    }
});

// ✅ Delete a user
router.delete("/:id", async (req, res) => {
    try {
        const deletedUser = await deleteUser(req.params.id);
        if (!deletedUser) return res.status(404).json({ message: "User not found" });
        res.json({ message: "User deleted successfully" });
    } catch (error) {
        res.status(500).json({ error: error.message });
    }
});

// ✅ Add or update game history entry
router.put("/:id/game-history", async (req, res) => {
    try {
        const updatedUser = await updateGameHistory(req.params.id, req.body);
        if (!updatedUser) return res.status(404).json({ message: "User not found" });
        res.json(updatedUser);
    } catch (error) {
        res.status(400).json({ error: error.message });
    }
});

// ✅ Delete a specific game history entry
router.delete("/:id/game-history/:gameroom_id", async (req, res) => {
    try {
        const updatedUser = await deleteGameHistory(req.params.id, req.params.gameroom_id);
        if (!updatedUser) return res.status(404).json({ message: "User not found" });
        res.json(updatedUser);
    } catch (error) {
        res.status(400).json({ error: error.message });
    }
});

module.exports = router;
