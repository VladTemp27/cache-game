const Cards = require('../models/Cards');

const getCards = async () => {
    return await Cards.find();
}

module.exports = { getCards};

