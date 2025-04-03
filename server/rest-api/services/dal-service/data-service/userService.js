const fs = require("fs");
const path = require("path");
const User = require("../models/User");

const userSeedPath = path.join(__dirname, "../data/user.json");

const updateUserJSON = async () => {
    const allUsers = await User.find();
    fs.writeFileSync(userSeedPath, JSON.stringify(allUsers, null, 2));
};

const upsertUser = async (userData) => {
    const { username } = userData;
    
    const existingUser = await User.findOne({ username });
    
    let updateData = { ...userData };
    
    if (existingUser && userData.total_score !== undefined && !userData.password) {
        updateData = {
            ...existingUser.toObject(),
            total_score: userData.total_score
        };
    }
    
    const result = await User.findOneAndUpdate(
        { username },
        updateData,
        { upsert: true, new: true, setDefaultsOnInsert: true }
    );
    
    await updateUserJSON();
    return result;
};

const getAllUsers = async () => {
    return await User.find();
};

const verifyUser = async (username) => {
    return await User.findOne({ username });
};

module.exports = { upsertUser, getAllUsers, verifyUser, updateUserJSON };