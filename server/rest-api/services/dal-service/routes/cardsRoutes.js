const express = require("express");
const { getCards } = require("../data-service/cardsService");

const router = express.Router();

router.get("/cards", async (req, res) => {
    try {
        const cards = await getCards();
        res.json(cards);
    } catch (error) {
        res.status(500).json({ error: error.message });
    }
});


module.exports = router;