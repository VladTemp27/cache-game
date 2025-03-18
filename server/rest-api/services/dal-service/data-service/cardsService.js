const Cards = require('../models/cardModel');

const getCards = async (Cards) => {
    return await Cards.find();
}

const upsertCard = async (cardData) => {
    const { pair_id } = cardData;
    return await Cards.findOneAndUpdate(
        { pair_id },
        cardData,
        { upsert: true, new: true, setDefaultsOnInsert: true }
    );
};

module.exports = { getCards };