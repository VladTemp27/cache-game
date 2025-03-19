const express = require("express");
const cors = require("cors");
const dotenv = require("dotenv");
const connectDB = require("./services/dal-service/config/db");
const authRouter = require("./services/auth-service/auth");
const userRoutes = require("./services/dal-service/routes/userRoutes");
const leaderboardRoutes = require("./services/dal-service/routes/leaderboardRoutes");
const seedDatabase = require("./services/dal-service/scripts/seedDB");

dotenv.config(); // Load environment variables

const app = express();
app.use(express.json());
app.use(cors());

// Health check route
app.get("/health", (req, res) => {
    res.send({ message: "Server is up and running" });
});

// Authentication routes
app.use("/auth", authRouter);

// Connect to MongoDB and seed the database before starting the server
connectDB()
    .then(async () => {
        console.log("✅ MongoDB Connected");

        // Seed database
        await seedDatabase();
        console.log("✅ Database Seeded Successfully");

        // Mount routes
        app.use("/users", userRoutes);
        app.use("/leaderboard", leaderboardRoutes);

        // Start the server
        const PORT = process.env.PORT || 8080;
        app.listen(PORT, () => {
            console.log(`🚀 Server is running on port ${PORT}`);
        });
    })
    .catch((error) => {
        console.error("❌ Failed to connect to MongoDB:", error);
        process.exit(1); // Exit process if DB connection fails
    });
