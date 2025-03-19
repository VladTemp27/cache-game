const mongoose = require("mongoose");

const CardFormat = new mongoose.Schema({
    pair_id: { type: Number, required: true },
    pair: {
        answer: { type: String, required: true },
        question: { type: String, required: true }
    }
});

module.exports = mongoose.models.Card || mongoose.model("Card", CardFormat);