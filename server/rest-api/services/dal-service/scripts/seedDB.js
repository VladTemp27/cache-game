const mongoose = require("mongoose");
const User = require("../models/User");
const Session = require("../models/Session");
require("dotenv").config();
const connectDB = require("../config/db");

const seedDatabase = async () => {
    await connectDB(); // Ensure DB is connected

    console.log("🔄 Seeding Database...");

    // Clear existing data
    await User.deleteMany({});
    await Session.deleteMany({});

    // Create test users
    const users = await User.insertMany([
        { username: "gibgib", password: "testpassword", total_score: 100 },
        { username: "testuser2", password: "testpassword", total_score: 200 },
        { username: "testuser3", password: "testpassword", total_score: 150 }
    ]);

    console.log("✅ Users seeded:", users.length);

    return users;
};

// Run script if executed directly
if (require.main === module) {
    seedDatabase()
        .then(() => {
            console.log("✅ Database seeding complete.");
            mongoose.connection.close();
        })
        .catch((err) => {
            console.error("❌ Seeding failed:", err);
            mongoose.connection.close();
        });
}

module.exports = seedDatabase;
