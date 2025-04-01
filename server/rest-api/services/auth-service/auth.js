const express = require("express");
const userService = require("../dal-service/data-service/userService")
const sessionService = require("../dal-service/data-service/sessionService"); //crazy path by marven lodi
const authRouter = express.Router();

/**
 * Login Route
 * Dis da 4 yall controller team
 * Request Type: POST
 * Full route: http://localhost:8080/auth/login
 * 
 * The  request must have a username and password in the request body
 * Format is:
 * {
    "username": "testuser",
    "password": "testpassword"
 * }
 */
authRouter.post("/login", async (req, res) => {
    const { username, password } = req.body;
    if (!username || !password) return res.status(400).json({ error: "Username and password required" });

    try {
        const user = await userService.verifyUser(username);
        if (!user) {
            return res.status(404).json({ error: "User not found" });
        }

        if (user.password !== password) {
            return res.status(401).json({ error: "Invalid credentials" });
        }

        // Pass user._id instead of username to sessionService.createSession
        const sessionId = await sessionService.createSession(user._id);

        res.json({ sessionId });
    } catch (error) {
        console.error("Error:", error);
        res.status(500).json({ error: "Internal Server Error" });
    }
});

//Testing middleware
authRouter.get("/protected", async (req, res) => {
    const sessionId = req.headers["authorization"];
    if (!sessionId) return res.status(401).json({ error: "Unauthorized" });

    const isValid = await sessionService.validateSession(sessionId);
    if (!isValid) return res.status(401).json({ error: "Invalid session" });

    res.json({ message: "You have access to this protected route!" });
});

/**
 * Logout Route
 * Request Type: POST
 * Full route: http://localhost:8080/auth/logout
 * 
 * The request must have the sessionId in the authorization header like so:
 *  headers: {
    'Authorization': sessionId
 * }
 */
authRouter.post("/logout", async (req, res) => {
    const sessionId = req.headers["authorization"];
    if (!sessionId) return res.status(400).json({ error: "Session ID required" });

    try {
        await sessionService.deleteSession(sessionId);
        res.json({ message: "Logged out" });
    } catch (error) {
        console.error("Error:", error);
        res.status(500).json({ error: "Internal Server Error" });
    }
});

/**
 * Middleware for authentication
 * To be added to the routes that will be used after login
 */
const authMiddleware = async (req, res, next) => {
    const sessionId = req.headers["authorization"];
    if (!sessionId) return res.status(401).json({ error: "Unauthorized" });

    try {
        const isValid = await sessionService.validateSession(sessionId);
        if (!isValid) return res.status(401).json({ error: "Unauthorized" });

        next();
    } catch (error) {
        console.error("Error:", error);
        res.status(500).json({ error: "Internal Server Error" });
    }
};

module.exports = { authRouter, authMiddleware };
