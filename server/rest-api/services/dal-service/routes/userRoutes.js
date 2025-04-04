const express = require("express");
const { upsertUser, getAllUsers, updateUserJSON } = require("../data-service/userService");

const router = express.Router();

// Create a new user
router.post("/createUser", async (req, res) => {
    try {
        const newUser = await upsertUser(req.body);
        await updateUserJSON();
        res.status(201).json(newUser);
    } catch (error) {
        res.status(400).json({ error: error.message });
    }
});

// Update an existing user
router.put("/updateUser", async (req, res) => {
    try {
        const updatedUser = await upsertUser(req.body);
        res.status(200).json(updatedUser);
    } catch (error) {
        res.status(400).json({ error: error.message });
    }
});

// Get all users
router.get("/getUsers", async (req, res) => {
    try {
        const users = await getAllUsers();
        res.json(users);
    } catch (error) {
        res.status(500).json({ error: error.message });
    }
});

module.exports = router;