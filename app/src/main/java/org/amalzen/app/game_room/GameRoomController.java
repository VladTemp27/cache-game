package org.amalzen.app.game_room;

import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.HBox;
import org.amalzen.app.Main;
import org.amalzen.app.ResourcePath;
import org.amalzen.app.audio.AudioHandler;
import org.amalzen.app.components.CardComponent;
import org.json.JSONArray;
import org.json.JSONObject;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Controller for the game room, handling card grid, game state and server communication
 */
public class GameRoomController {
    private static final Logger LOGGER = Logger.getLogger(GameRoomController.class.getName());
    private static final int ROWS = 2;
    private static final int COLUMNS = 8;

    public List<CardComponent> cardComponents = new ArrayList<>();

    @FXML
    private AnchorPane gameRoomPane;
    @FXML
    private Label roundNumber;
    @FXML
    private Label timePerTurn; // this should be time remaining
    @FXML
    private Label rivalScore;
    @FXML
    private Label homeScore;
    @FXML
    private Button cancelButton;
    @FXML
    private HBox HBox1;
    @FXML
    private HBox HBox2;

    @FXML
    private Label whoseTurn;

    // Game state variables
    private GameRoomModel gameRoom;
    private String gameId;
    private String username;
    private int playerId;

    private boolean isMyTurn = false;
    private boolean[] pairedCards = new boolean[ROWS * COLUMNS];
    private String[] cardTexts = new String[ROWS * COLUMNS];

    @FXML
    public void initialize() {
        AudioHandler.playSound(ResourcePath.GAME_ROOM_MUSIC.getPath());
        // Setup UI components
        cancelButton.setOnMouseClicked(event -> {
            if (gameRoom != null) {
                gameRoom.sendQuit();
            }
            // TODO this modal still needs controller to properly close the connection
            Main.showModals(ResourcePath.EXIT_MODAL.getPath(), gameRoomPane);
        });

        // Create card grid
        createCardGrid();
    }

    public void setGameParameters(String gameId, int playerId, String username) {
        this.gameId = gameId;
        this.playerId = playerId;
        this.username = username;

        LOGGER.info("Game parameters set: gameId=" + gameId +
                ", playerId=" + playerId + ", username=" + username);

        // Initialize or reconnect
        if (gameRoom != null) {
            shutdown();
        }
        initializeGameRoom();
    }

    private void createCardGrid() {
        try {
            for (int row = 0; row < ROWS; row++) {
                HBox currentHBox = (row == 0) ? HBox1 : HBox2;

                for (int col = 0; col < COLUMNS; col++) {
                    FXMLLoader loader = new FXMLLoader(getClass().getResource(ResourcePath.CARD.getPath()));
                    Node cardNode = loader.load();
                    CardComponent cardComponent = loader.getController();

                    int cardId = row * COLUMNS + col;
                    cardComponent.setCardId(cardId);

                    // Set card click behavior
                    final int index = cardId;
                    cardComponent.cardButton.setOnMouseClicked(event -> handleCardClick(index));

                    cardComponents.add(cardComponent);
                    currentHBox.getChildren().add(cardNode);
                }
            }
        } catch (IOException e) {
            LOGGER.log(Level.SEVERE, "Error creating card grid", e);
        }
    }

    private void handleCardClick(int cardIndex) {
        // Check if this is a valid move
        CardComponent card = cardComponents.get(cardIndex);
        if (isMyTurn && !pairedCards[cardIndex] && !card.isFlipped()) {
            LOGGER.info("Sending flip for card: " + cardIndex);
            card.flipCard();
            card.setCardLabel(cardTexts[cardIndex]);
            gameRoom.sendFlip(cardIndex);
        }
    }

    private void initializeGameRoom() {
        LOGGER.info("Initializing game room: " + gameId + ", Player: " + playerId);

        gameRoom = new GameRoomModel(gameId, playerId, username)
                .onConnected(() -> {
                    Platform.runLater(() -> LOGGER.info("Connected to game server"));
                })
                .onGameStateUpdate(this::handleGameStateUpdate)
                .onConnectionClosed(() -> {
                    Platform.runLater(() -> LOGGER.info("Connection closed"));
                })
                .onError(error -> {
                    Platform.runLater(() -> LOGGER.log(Level.SEVERE, "Error: " + error.getMessage(), error));
                });

        // Connect to the server
        gameRoom.connect().exceptionally(ex -> {
            Platform.runLater(() -> LOGGER.log(Level.SEVERE, "Connection failed", ex));
            return null;
        });
    }

    private void handleGameStateUpdate(JSONObject gameState) {
        LOGGER.fine("Game state update: " + gameState.toString());

        Platform.runLater(() -> {
            // Update game UI elements
            updateGameUI(gameState);

            // Handle game state changes
            if (gameState.has("status") && gameState.getString("status").equals("match_end")) {
                handleMatchEnd(gameState);
                return;
            }

            // Handle move results
            if (gameState.has("action") && gameState.getString("action").equals("move")) {
                boolean matched = gameState.optBoolean("matched", false);
                if (matched) {
                    LOGGER.info("Server confirmed a match");
                    // Matched cards are handled in updateCardStates via pairedArray
                } else {
                    LOGGER.info("Server confirmed no match");

                    // For non-matching cards, we need to explicitly handle flipping them back
                    // after a short delay for player to see the cards
                    if (gameState.has("flippedCards")) {
                        JSONArray flippedCards = gameState.getJSONArray("flippedCards");
                        List<Integer> indicesToFlipBack = new ArrayList<>();

                        for (int i = 0; i < flippedCards.length(); i++) {
                            indicesToFlipBack.add(flippedCards.getInt(i));
                        }

                        // Delay flipping back to give player time to see cards
                        new java.util.Timer().schedule(
                                new java.util.TimerTask() {
                                    @Override
                                    public void run() {
                                        Platform.runLater(() -> {
                                            for (int index : indicesToFlipBack) {
                                                CardComponent card = cardComponents.get(index);
                                                if (card.isFlipped() && !pairedCards[index]) {
                                                    card.flipCard();
                                                }
                                            }
                                        });
                                    }
                                },
                                1000 // 1-second delay
                        );
                    }
                }
            }
        });
    }

    private void updateGameUI(JSONObject gameState) {
        // Update scores
        if (gameState.has("yourScore")) homeScore.setText(String.valueOf(gameState.getInt("yourScore")));
        if (gameState.has("oppScore")) rivalScore.setText(String.valueOf(gameState.getInt("oppScore")));

        // Update round and timer
        if (gameState.has("round")) roundNumber.setText("Round " + gameState.getInt("round"));
        if (gameState.has("timer")) timePerTurn.setText(gameState.getInt("timer") + "s");

        // Update turn information
        if (gameState.has("whoseTurn")) {
            String turn = gameState.getString("whoseTurn");
            isMyTurn = "your turn".equals(turn);

            // Update the whoseTurn label
            if (isMyTurn) {
                whoseTurn.setText("YOUR TURN");
                whoseTurn.setStyle("-fx-text-fill: #4CAF50;"); // Green color
            } else {
                whoseTurn.setText("OPPONENT'S TURN");
                whoseTurn.setStyle("-fx-text-fill: #F44336;"); // Red color
            }

            LOGGER.info("Turn status: " + turn);
        }

        // Update card states based on server data
        updateCardStates(gameState);
    }

    private void updateCardStates(JSONObject gameState) {
        LOGGER.fine("Updating card states from: " + gameState);

        if (gameState.has("cards") && gameState.has("paired")) {
            JSONArray cardsArray = gameState.getJSONArray("cards");
            JSONArray pairedArray = gameState.getJSONArray("paired");

            // Get currently flipped cards from server
            List<Integer> flippedCards = new ArrayList<>();
            if (gameState.has("flippedCards")) {
                JSONArray flippedArray = gameState.getJSONArray("flippedCards");
                for (int i = 0; i < flippedArray.length(); i++) {
                    flippedCards.add(flippedArray.getInt(i));
                }
                LOGGER.fine("Server reported flipped cards: " + flippedCards);
            } else if (gameState.has("flippedCard")) {
                // Single flipped card format
                flippedCards.add(gameState.getInt("flippedCard"));
                LOGGER.fine("Server reported flipped card: " + flippedCards.get(0));
            }

            // Update all cards based on server state
            for (int i = 0; i < Math.min(cardsArray.length(), cardComponents.size()); i++) {
                String cardValue = cardsArray.getString(i);
                boolean isPaired = pairedArray.getBoolean(i);
                CardComponent card = cardComponents.get(i);

                // Store card text
                cardTexts[i] = cardValue;

                // Update card state based on server data
                if (isPaired) {
                    // Paired cards should be flipped and show value
                    pairedCards[i] = true;
                    if (!card.isFlipped()) {
                        card.flipCard();
                        card.setCardLabel(cardValue);
                    }
                } else if (flippedCards.contains(i)) {
                    // This card is currently flipped
                    if (!card.isFlipped()) {
                        card.flipCard();
                        card.setCardLabel(cardValue);
                    }
                } else if (card.isFlipped() && !isPaired) {
                    // This card should not be flipped
                    card.flipCard(); // Flip back to face-down
                }
            }
        }
    }

    private void handleMatchEnd(JSONObject gameState) {
        int winner = gameState.optInt("winner", -1);

        if (winner == playerId) {
            // Show victory modal
            try {
                FXMLLoader loader = new FXMLLoader(getClass().getResource(ResourcePath.VICTORY_MODAL.getPath()));
                Parent victoryModalRoot = loader.load();
                gameRoomPane.getChildren().add(victoryModalRoot);

                // Store controller reference
                gameRoomPane.getProperties().put("controller", this);
            } catch (IOException e) {
                LOGGER.log(Level.SEVERE, "Error showing victory modal", e);
            }
        } else if (winner != -1) {
            // Show defeat modal
            try {
                FXMLLoader loader = new FXMLLoader(getClass().getResource(ResourcePath.DEFEAT_MODAL.getPath()));
                Parent defeatModalRoot = loader.load();
                gameRoomPane.getChildren().add(defeatModalRoot);

                // Store controller reference
                gameRoomPane.getProperties().put("controller", this);
            } catch (IOException e) {
                LOGGER.log(Level.SEVERE, "Error showing defeat modal", e);
            }
        } else {
            // It's a tie
            LOGGER.info("Game ended in a tie");
        }
    }

    public void shutdown() {
        if (gameRoom != null) {
            try {
                gameRoom.sendQuit();
                gameRoom.disconnect();
                gameRoom.close();
                gameRoom = null;
            } catch (Exception e) {
                LOGGER.log(Level.WARNING, "Error during shutdown", e);
            }
        }
    }
}