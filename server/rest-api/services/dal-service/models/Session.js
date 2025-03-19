const mongoose = require("mongoose");

const sessionFormat = new mongoose.Schema({ //bat hindi schema pinangalan mo marven
    user_id: { type: mongoose.Schema.Types.ObjectId, ref: "User", required: true },
    created_at: { type: Date, default: Date.now },
    last_active_at: { type: Date, default: Date.now },
    expires_at: { type: Date, default: () => new Date(Date.now() + 24 * 60 * 60 * 1000) } // 24hr expirazion
});

sessionFormat.index({ expires_at: 1 }, { expireAfterSeconds: 0 });

module.exports = mongoose.model("Session", sessionFormat);
