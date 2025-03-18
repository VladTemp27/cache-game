require("dotenv").config();
const mongoose = require("mongoose");

const connectDB = async () => {
    console.log("Connecting to MongoDB...", process.env.MONGO_URI);

    try {
        await mongoose.connect(process.env.MONGO_URI, {
            useNewUrlParser: true,
            useUnifiedTopology: true
        });
        console.log(" Succesfully connected to MongoDB");   
    } catch (error) {
        console.error("MongoDB connection error:", error.message);
        process.exit(1);
    }
};

module.exports = connectDB;
