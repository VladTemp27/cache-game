const mongoose = require("mongoose");

const cardSchema = new mongoose.Schema({
  _id: { type: mongoose.Schema.Types.ObjectId, required: true },
  pair_id: { type: Number, required: true },
  pair: {
    answer: { type: String, required: true },
    question: { type: String, required: true }
  }
});

module.exports = mongoose.model("Card", cardSchema);