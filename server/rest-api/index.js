const express = require('express');

const app = express();
app.use(express.json());

app.get('/health', (req,res) => {
    res.send({message : "Server is up and running"});
})

app.listen(8000, () => {
    console.log("Server is running on port 8000")
})