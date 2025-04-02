package org.amalzen.app.game_room;

import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
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

    @FXML private AnchorPane gameRoomPane;
    @FXML private Label roundNumber;
    @FXML private Label timePerTurn;
    @FXML private Label rivalScore;
    @FXML private Label homeScore;
    @FXML private Button cancelButton;
    @FXML private HBox HBox1;
    @FXML private HBox HBox2;
    @FXML private Label whoseTurn;

    // Game state variables
    private GameRoomModel gameRoom;
    private String gameId;
    private String username;
    private int playerId;

    private boolean isMyTurn = false;
    private boolean[] pairedCards = new boolean[ROWS * COLUMNS];
    private String[] cardTexts = new String[ROWS * COLUMNS];
    private Integer lastFlippedCardIndex = null;
    private boolean waitingForServerResponse = false;

    @FXML
    public void initialize() {
        AudioHandler.playSound(ResourcePath.GAME_ROOM_MUSIC.getPath());
        // Setup UI components
        cancelButton.setOnMouseClicked(event -> {
            if (gameRoom != null) {
                gameRoom.sendQuit();
            }
            Main.showModals(ResourcePath.EXIT_MODAL.getPath(), gameRoomPane);
        });
    }

    public void setGameParameters(String gameId, int playerId, String username) {
        this.gameId = gameId;
        this.playerId = playerId;
        this.username = username;

        LOGGER.info("Game parameters set: gameId=" + gameId +
                ", playerId=" + playerId + ", username=" + username);

        if (gameRoom != null) {
            shutdown();
        }
        initializeGameRoom();
    }

    private void createCardGrid() {
        try {
            cardComponents.clear();
            HBox1.getChildren().clear();
            HBox2.getChildren().clear();

            for (int row = 0; row < ROWS; row++) {
                HBox currentRow = (row == 0) ? HBox1 : HBox2;

                for (int col = 0; col < COLUMNS; col++) {
                    int cardIndex = row * COLUMNS + col;

                    FXMLLoader loader = new FXMLLoader(getClass().getResource(ResourcePath.CARD.getPath()));
                    Parent cardRoot = loader.load();
                    CardComponent card = loader.getController();

                    card.setCardId(cardIndex);

                    // Add card click handler directly to the card component
                    cardRoot.setOnMouseClicked(event -> {
                        if (canFlipCard(cardIndex)) {
                            handleCardFlip(cardIndex);
                        }
                    });

                    currentRow.getChildren().add(cardRoot);
                    cardComponents.add(card);
                }
            }

            LOGGER.info("Card grid created with " + cardComponents.size() + " cards");
        } catch (IOException e) {
            LOGGER.log(Level.SEVERE, "Failed to create card grid", e);
        }
    }

    private boolean canFlipCard(int cardIndex) {
        if (!isMyTurn || waitingForServerResponse || pairedCards[cardIndex]) {
            return false;
        }

        CardComponent card = cardComponents.get(cardIndex);
        return !card.isFlipped();
    }

    private void handleCardFlip(int cardIndex) {
        // Send flip request to server
        gameRoom.sendFlip(cardIndex);

        // Track card state locally
        if (lastFlippedCardIndex == null) {
            lastFlippedCardIndex = cardIndex;
        } else {
            waitingForServerResponse = true;
        }

        // Flip card and show content
        CardComponent card = cardComponents.get(cardIndex);
        if (!card.isFlipped()) {
            card.flipCard();
        }
    }

    private void initializeGameRoom() {
        LOGGER.info("Initializing game room: " + gameId + ", Player: " + playerId);

        gameRoom = new GameRoomModel(gameId, playerId, username)
                .onGameStateUpdate(this::handleGameStateUpdate)
                .onConnected(() -> {
                    LOGGER.info("Connected to game server");
                    Platform.runLater(() -> {
                        whoseTurn.setText("Connected, waiting for opponent...");
                    });
                })
                .onConnectionClosed(() -> {
                    LOGGER.info("Connection to game server closed");
                    Platform.runLater(() -> {
                        whoseTurn.setText("Connection lost!");
                    });
                })
                .onError(error -> {
                    LOGGER.log(Level.SEVERE, "Game room error", error);
                    Platform.runLater(() -> {
                        whoseTurn.setText("Error: " + error.getMessage());
                    });
                });

        gameRoom.connect().exceptionally(ex -> {
            LOGGER.log(Level.SEVERE, "Failed to connect to game server", ex);
            Platform.runLater(() -> {
                whoseTurn.setText("Failed to connect: " + ex.getMessage());
            });
            return null;
        });
    }

    private void handleGameStateUpdate(JSONObject gameState) {
        LOGGER.fine("Game state update: " + gameState.toString());

        Platform.runLater(() -> {
            try {
                String eventType = gameState.optString("event", "unknown");

                switch (eventType) {
                    case "game_ready":
                        handleGameReadyEvent(gameState);
                        break;
                    case "players_ready":
                        whoseTurn.setText("Both players connected. Game starting!");
                        updateGameUI(gameState);
                        break;
                    case "cards_matched":
                        handleCardsMatchedEvent(gameState);
                        break;
                    case "turn_switch":
                        handleTurnSwitchEvent(gameState);
                        break;
                    case "timer_update":
                        if (gameState.has("timer")) {
                            timePerTurn.setText(gameState.getInt("timer") + "s");
                        }
                        break;
                    case "game_end":
                        handleGameEndEvent(gameState);
                        break;
                    default:
                        LOGGER.warning("Unknown event type: " + eventType);
                }
            } catch (Exception e) {
                LOGGER.log(Level.SEVERE, "Error processing game state: " + gameState, e);
            }
        });
    }

    private void handleGameReadyEvent(JSONObject gameState) {
        // Process card data
        if (gameState.has("cards")) {
            Object cardsObj = gameState.get("cards");

            if (cardsObj instanceof JSONArray) {
                JSONArray cardsArray = (JSONArray) cardsObj;
                for (int i = 0; i < Math.min(cardsArray.length(), cardTexts.length); i++) {
                    cardTexts[i] = cardsArray.getString(i);
                }
            } else if (cardsObj instanceof String[]) {
                String[] cardsArray = (String[]) cardsObj;
                for (int i = 0; i < Math.min(cardsArray.length, cardTexts.length); i++) {
                    cardTexts[i] = cardsArray[i];
                }
            } else {
                LOGGER.warning("Unexpected cards data type: " + cardsObj.getClass().getName());
            }
        }

        // Create the card grid
        createCardGrid();

        // Preload all card labels
        for (int i = 0; i < cardComponents.size(); i++) {
            cardComponents.get(i).setCardLabel(cardTexts[i]);
        }

        // Initialize UI elements
        String opponentName = gameState.optString("opponentName", "Opponent");
        int timeDuration = gameState.optInt("timeDuration", 30);

        whoseTurn.setText("Game is ready. Waiting for players...");
        timePerTurn.setText(timeDuration + "s");
        roundNumber.setText("Round 1");
        homeScore.setText("0");
        rivalScore.setText("0");

        LOGGER.info("Game ready with opponent: " + opponentName);
    }

    private void handleCardsMatchedEvent(JSONObject gameState) {
        // Update UI based on matched cards (scores, turn info)
        updateGameUI(gameState);

        // Reset waiting state to allow player to flip more cards when it's their turn
        waitingForServerResponse = false;
        lastFlippedCardIndex = null;

        // Process matched cards information
        if (gameState.has("matchedPair")) {
            JSONArray matchedPair = gameState.getJSONArray("matchedPair");
            if (matchedPair.length() == 2) {
                int card1 = matchedPair.getInt(0);
                int card2 = matchedPair.getInt(1);

                // Mark cards as paired and ensure they stay flipped
                pairedCards[card1] = true;
                pairedCards[card2] = true;

                // Apply visual indication that cards are matched
                Platform.runLater(() -> {
                    if (card1 < cardComponents.size()) {
                        cardComponents.get(card1).cardButton.getStyleClass().add("matched-card");
                    }
                    if (card2 < cardComponents.size()) {
                        cardComponents.get(card2).cardButton.getStyleClass().add("matched-card");
                    }
                });
            }
        } else if (gameState.has("paired")) {
            // Alternative format - handle boolean array of all paired cards
            Object pairedObj = gameState.get("paired");

            if (pairedObj instanceof JSONArray) {
                JSONArray pairedArray = (JSONArray) pairedObj;
                for (int i = 0; i < Math.min(pairedArray.length(), pairedCards.length); i++) {
                    boolean newPaired = pairedArray.getBoolean(i);
                    boolean wasPaired = pairedCards[i];

                    // If card was newly paired in this move
                    if (newPaired && !wasPaired) {
                        final int index = i; // For lambda
                        Platform.runLater(() -> {
                            if (index < cardComponents.size()) {
                                cardComponents.get(index).cardButton.getStyleClass().add("matched-card");
                            }
                        });
                    }

                    pairedCards[i] = newPaired;
                }
            } else if (pairedObj instanceof boolean[]) {
                boolean[] pairedArr = (boolean[]) pairedObj;
                for (int i = 0; i < Math.min(pairedArr.length, pairedCards.length); i++) {
                    boolean newPaired = pairedArr[i];
                    boolean wasPaired = pairedCards[i];

                    // If card was newly paired in this move
                    if (newPaired && !wasPaired) {
                        final int index = i; // For lambda
                        Platform.runLater(() -> {
                            if (index < cardComponents.size()) {
                                cardComponents.get(index).cardButton.getStyleClass().add("matched-card");
                            }
                        });
                    }

                    pairedCards[i] = pairedArr[i];
                }
            }
        }

        // Ensure all cards are in the correct visual state
        updateCardVisualStates();
    }

    /**
     * Ensures all cards show the correct visual state:
     * - Matched cards stay face up
     * - Non-matched cards are face down unless currently being viewed
     */
    private void updateCardVisualStates() {
        Platform.runLater(() -> {
            for (int i = 0; i < cardComponents.size(); i++) {
                CardComponent card = cardComponents.get(i);

                // If card is paired, ensure it's face up
                if (i < pairedCards.length && pairedCards[i]) {
                    if (!card.isFlipped()) {
                        card.flipCard();
                    }
                }
            }
        });
    }

    private void handleTurnSwitchEvent(JSONObject gameState) {
        // Update UI for turn switch
        updateGameUI(gameState);

        // Reset waiting state
        waitingForServerResponse = false;
        lastFlippedCardIndex = null;

        // Flip back non-matched cards
        if (gameState.has("flippedCards")) {
            JSONArray flippedCards = gameState.getJSONArray("flippedCards");
            for (int i = 0; i < flippedCards.length(); i++) {
                int cardIndex = flippedCards.getInt(i);
                if (!pairedCards[cardIndex]) {
                    flipBackCardWithDelay(cardIndex);
                }
            }
        } else {
            // If no specific flipped cards provided, check for any that need to be flipped back
            for (int i = 0; i < cardComponents.size(); i++) {
                if (!pairedCards[i] && cardComponents.get(i).isFlipped()) {
                    flipBackCardWithDelay(i);
                }
            }
        }
    }

    private void flipBackCardWithDelay(int cardIndex) {
        CardComponent card = cardComponents.get(cardIndex);
        if (card.isFlipped()) {
            // Delay flip to let player see the card
            new Thread(() -> {
                try {
                    Thread.sleep(800);
                    Platform.runLater(card::flipCard);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            }).start();
        }
    }

    private void handleGameEndEvent(JSONObject gameState) {
        int winner = gameState.optInt("winner", -1);
        String message;

        if (winner == playerId) {
            message = "You won!";
        } else if (winner != -1) {
            message = "You lost!";
        } else {
            message = "It's a tie!";
        }

        whoseTurn.setText("Game Over - " + message);

        // Display final scores
        if (gameState.has("yourScore")) homeScore.setText(String.valueOf(gameState.getInt("yourScore")));
        if (gameState.has("oppScore")) rivalScore.setText(String.valueOf(gameState.getInt("oppScore")));
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
            String currentTurn = gameState.getString("whoseTurn");
            isMyTurn = username.equals(currentTurn);

            if (isMyTurn) {
                whoseTurn.setText("Your turn");
            } else {
                whoseTurn.setText(currentTurn + "'s turn");
            }
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