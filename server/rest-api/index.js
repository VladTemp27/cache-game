require("dotenv").config();  // Load environment variables at the very top

const express = require("express");
const cors = require("cors");
const connectDB = require("./services/dal-service/config/db");
const {authRouter, authMiddleware} = require('./services/auth-service/auth');
const userRoutes = require("./services/dal-service/routes/userRoutes");
const cardsRoutes = require("./services/dal-service/routes/cardsRoutes");
const gameHistoryRoutes = require("./services/dal-service/routes/gameHistoryRoutes");
const seedDatabase = require("./services/dal-service/scripts/seed");

const app = express();
app.use(express.json());

app.get("/health", (req, res) => {
    res.send({message: "Server is up and running"});
});

const startServer = async () => {
        try {

            await connectDB();
            await seedDatabase();

            const PORT = process.env.PORT || 8080;

            app.use("/auth", authRouter);
            app.use("/users", authMiddleware,userRoutes);
            app.use("/cards", authMiddleware, cardsRoutes);
            app.use("/gameHistory", authMiddleware, gameHistoryRoutes);

            app.listen(PORT, () => {
                console.log(`Server is running on port ${PORT}`);
            });
        } catch (error) {
            console.error("Error starting server", error);
            process.exit(1);
        }
    }
;

startServer();