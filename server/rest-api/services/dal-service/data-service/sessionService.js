const Session = require("../models/Session");

// Create new sesh
const createSession = async (userId) => {
    const session = new Session({ user_id: userId });

    const savedSession = await session.save();
    return savedSession._id.toString();
};

// Retrieve a sesh
const getSession = async (sessionId) => {
    return await Session.findOne({ _id: sessionId, expires_at: { $gt: new Date() } });
};

// Check a session (for auth middleware)
const validateSession = async (sessionId) => {
    try {
        const session = await Session.findById(sessionId);

        if (!session) return false;

        // OPTIONAL: If using expiration, check if the session is still valid
        if (session.expires_at && session.expires_at < new Date()) {
            return false; // Session expired
        }

        return true; // Session is valid
    } catch (error) {
        console.error("Error validating session:", error);
        return false; // In case of an error, assume session is invalid
    }
};

//delete a sesh by id
const deleteSession = async (sessionId) => {
    const result = await Session.deleteOne({ _id: sessionId });
    return result.deletedCount > 0;
};

module.exports = { createSession, getSession, validateSession, deleteSession};
