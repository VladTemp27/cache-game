const express = require('express');
const authRouter = require("./services/auth-service/auth");

const app = express();
app.use(express.json());

app.get('/health', (req,res) => {
    res.send({message : "Server is up and running"});
})

app.use('/auth', authRouter);

app.listen(8080, () => {
    console.log("Server is running on port 8080")
})