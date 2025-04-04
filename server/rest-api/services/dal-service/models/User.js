const mongoose = require("mongoose");

const UserFormat = new mongoose.Schema({
    username: { type: String, required: true, unique: true },
    password: { type: String, required: true }, 
    token: { type: String },
    total_score: { type: Number, default: 0 }
});

module.exports = mongoose.model("User", UserFormat);