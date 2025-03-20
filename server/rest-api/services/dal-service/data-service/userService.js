const fs = require("fs");
const path = require("path");
const User = require("../models/User");

const userSeedPath = path.join(__dirname, "../data/user.json");

const updateUserJSON = async () => {
    const allUsers = await User.find();
    fs.writeFileSync(userSeedPath, JSON.stringify(allUsers, null, 2));
};

// Upsert user
const upsertUser = async (userData) => {
    const { username } = userData;
    const result = await User.findOneAndUpdate(
        { username },
        userData,
        { upsert: true, new: true, setDefaultsOnInsert: true }
    );
    await updateUserJSON();
    return result;
};

// Get all users
const getAllUsers = async () => {
    return await User.find();
};

module.exports = { upsertUser, getAllUsers, updateUserJSON };