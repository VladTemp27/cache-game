require("dotenv").config();  // Load environment variables at the very top

const express = require("express");
const cors = require("cors");
const connectDB = require("./services/dal-service/config/db"); 
const userRoutes = require("./services/dal-service/routes/userRoutes");
const leaderboardRoutes = require("./services/dal-service/routes/leaderboardRoutes");
const seedDatabase = require("./services/dal-service/scripts/seedDB");

const app = express();
app.use(express.json());
app.use(cors());

//Connect to MongoDB before starting the server
connectDB()
    .then(async () => {  //Make this async to await seeding
        console.log("✅ MongoDB Connected");

        //Seed the database
        await seedDatabase();
        console.log("Database Seeded Successfully");

        const PORT = process.env.PORT || 8080;
        app.get("/health", (req, res) => {
            res.send({ message: "Server is up and running" });
        });

        app.use("/users", userRoutes);
        app.use("/leaderboard", leaderboardRoutes);

        app.listen(PORT, () => {
            console.log(`Server is running on port ${PORT}`);
        });
    })
    .catch((error) => {
        console.error("❌ Failed to connect to MongoDB:", error);
        process.exit(1);  // ✅ Prevent the server from starting if DB fails
    });