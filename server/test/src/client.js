const WebSocket = require('ws');
const readline = require('readline');

const gameID = process.argv[2]; // Pass gameID as a command-line argument
const player = process.argv[3]; // Pass player identifier as a command-line argument
const username = process.argv[4]; // Pass username as a command-line argument

const ws = new WebSocket(`ws://localhost:8082/ws?gameID=${gameID}&player=${player}&username=${username}`);

ws.on('open', () => {
  console.log('Connected to the server');
  startInputThread();
});

ws.on('message', (data) => {
  const gameState = JSON.parse(data);
  console.clear();

  // Log the received payload for debugging
  console.log('[DEBUG] Received payload:', gameState);

  switch (gameState.event) {
    case 'game_ready':
      console.log('Game Ready:');
      console.log(`Cards: ${JSON.stringify(gameState.cards)}`);
      console.log(`Opponent's Name: ${gameState.opponentName}`);
      console.log(`Time Duration: ${gameState.timeDuration} seconds`);
      break;

    case 'players_ready':
      console.log('Players Ready:');
      console.log(`Your Score: ${gameState.yourScore}`);
      console.log(`Opponent's Score: ${gameState.oppScore}`);
      console.log(`Game Status: ${gameState.gameStatus}`);
      console.log(`Whose Turn: ${gameState.whoseTurn}`);
      break;

    case 'cards_matched':
      console.log('Match Event:');
      console.log(`Your Score: ${gameState.yourScore}`);
      console.log(`Opponent's Score: ${gameState.oppScore}`);
      console.log(`Paired Cards: ${JSON.stringify(gameState.paired)}`);
      console.log(`Whose Turn: ${gameState.whoseTurn}`);
      break;

    case 'turn_switch':
      console.log('Turn Switch Event:');
      console.log(`Round: ${gameState.round}`);
      console.log(`Whose Turn: ${gameState.whoseTurn}`);
      break;

    case 'game_end':
      console.log('Game End:');
      console.log(`Game Status: ${gameState.gameStatus}`);
      if (gameState.winner === -1) {
        console.log('Game is Tied');
      } else {
        console.log(`Winner: ${gameState.usernames[gameState.winner]}`);
      }
      console.log(`Final Score: ${gameState.scores[0]} - ${gameState.scores[1]}`);
      break;

    default:
      console.log('Unknown event received:', gameState.event);
      break;
  }
});

ws.on('close', () => {
  console.log('Disconnected from the server');
});

ws.on('error', (error) => {
  console.error('WebSocket error:', error);
});

function flipCard(cardIndex) {
  const message = JSON.stringify({
    action: 'flip',
    cardIndex: cardIndex
  });
  ws.send(message);
  console.log(`Sent flip action for card index: ${cardIndex}`);
}

function startInputThread() {
  const rl = readline.createInterface({
    input: process.stdin,
    output: process.stdout
  });

  function promptFlip() {
    rl.question('Enter card index to flip: ', (answer) => {
      const cardIndex = parseInt(answer, 10);
      if (!isNaN(cardIndex)) {
        flipCard(cardIndex);
      } else {
        console.log('Invalid card index');
      }
      promptFlip(); // Prompt again for the next flip
    });
  }

  promptFlip();
}