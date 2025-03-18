const express = require("express");
const somethingDal = require("./services/something/seomthing"); //tbd after marven lodi
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
        const isValid = await somethingDal.verifyUser(username, password);
        if (!isValid) return res.status(401).json({ error: "Invalid credentials" });

        const sessionId = await dal.createSession(username);

        res.json({ sessionId });
    } catch (error) {
        console.error("Error:", error);
        res.status(500).json({ error: "Internal Server Error" });
    }
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
        await somethingDal.deleteSession(sessionId);
        res.json({ message: "Logged out" });
    } catch (error) {
        console.error("Error:", error);
        res.status(500).json({ error: "Internal Server Error" });
    }
});

module.exports = authRouter;
