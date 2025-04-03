package org.amalzen.app.game_room;

import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.HBox;
import javafx.util.Duration;
import org.amalzen.app.Main;
import org.amalzen.app.ResourcePath;
import org.amalzen.app.components.CardComponent;
import org.json.JSONArray;
import org.json.JSONObject;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.logging.Level;
import java.util.logging.Logger;

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
    private Label timePerTurn;
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

    private GameRoomModel gameRoom;
    private String gameId;
    private String username;
    private int playerId;

    private Timeline timer;
    private boolean isMyTurn = false;
    private boolean[] pairedCards = new boolean[ROWS * COLUMNS];    // Track paired cards
    private String[] cardTexts = new String[ROWS * COLUMNS];
    private boolean waitingForServerResponse = false;
    private Integer lastFlippedCardIndex = null;
    private Integer firstFlippedCardIndex = null;
    private Integer secondFlippedCardIndex = null;

    @FXML
    public void initialize() {
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
        // Do not allow flipping of cards that are already paired
        if (!isMyTurn || waitingForServerResponse || pairedCards[cardIndex]) {
            LOGGER.fine("Cannot flip card " + cardIndex + ": myTurn=" + isMyTurn +
                    ", waiting=" + waitingForServerResponse +
                    ", paired=" + pairedCards[cardIndex]);
            return false;
        }

        CardComponent card = cardComponents.get(cardIndex);
        return !card.isFlipped();
    }

    private void handleCardFlip(int cardIndex) {
        if (!canFlipCard(cardIndex)) {
            return;
        }

        // Send flip request to server
        gameRoom.sendFlip(cardIndex);

        CardComponent card = cardComponents.get(cardIndex);
        card.flipCard();

        if (lastFlippedCardIndex == null) {
            lastFlippedCardIndex = cardIndex;
            firstFlippedCardIndex = cardIndex;
        } else {
            secondFlippedCardIndex = cardIndex;
            waitingForServerResponse = true;
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
        // Log the complete game state JSON object for debugging
        LOGGER.fine("Game state update: " + gameState.toString());
        Platform.runLater(() -> {
            try {
                String eventType = gameState.optString("event", "unknown");

                LOGGER.info("Game state update:" + gameState.toString());
                LOGGER.info("Event type: " + eventType);

                switch (eventType) {
                    case "game_ready":
                        handleGameReadyEvent(gameState);
                        break;
                    case "players_ready":
                        whoseTurn.setText("Both players connected. Game starting!");
                        updateGameUI(gameState);
                        break;
                    case "card_flip":
                        handleCardFlipEvent(gameState);
                        break;
                    case "cards_matched":
                        handleCardsMatchedEvent(gameState);
                        break;
                    case "turn_switch":
                        handleTurnSwitchEvent(gameState);
                        break;
                    case "timer_update":
                        runTimer(gameState);
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

    private void runTimer(JSONObject gameState) {
        if (gameState.has("timer")) {
            AtomicInteger time = new AtomicInteger(gameState.getInt("timer"));
            timePerTurn.setText(time + "s");

            if (timer != null) {
                timer.stop();
            }

            timer = new Timeline(new KeyFrame(Duration.seconds(1), event -> {
                time.getAndDecrement();
                timePerTurn.setText(time + "s");
                if (time.get() <= 0) {
                    timer.stop();
                }
            }));
            timer.setCycleCount(time.get());
            timer.play();
        }
    }

    // This will flip the cards that the opponent has flipped
    private void handleCardFlipEvent(JSONObject gameState) {
        int cardIndex = gameState.getInt("flipped");

        // Flip card and show content
        CardComponent card = cardComponents.get(cardIndex);
        if (!card.isFlipped()) {
            card.flipCard();
        }
    }

    // This event provides the initial game state when the game is ready, such as the cards, opponentName, and time duration
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

    // This event happens when you or an opponent match two cards
    private void handleCardsMatchedEvent(JSONObject gameState) {
        LOGGER.info("Cards matched event received: " + gameState);

        // Update UI based on game state
        updateGameUI(gameState);

        // Reset waiting state
        waitingForServerResponse = false;
        lastFlippedCardIndex = null;

        // Process matched cards information
        if (gameState.has("paired")) {
            try {
                Object pairedObj = gameState.get("paired");
                LOGGER.info("Paired object type: " + pairedObj.getClass().getName());

                // Handle both JSONArray and primitive boolean array
                boolean[] pairedValues = (boolean[]) pairedObj;

                for (int i = 0; i < Math.min(pairedValues.length, pairedCards.length); i++) {
                    final int cardIndex = i;
                    final boolean isPaired = pairedValues[i];

                    // Update the paired status
                    pairedCards[i] = isPaired;

                    // Ensure paired cards stay flipped and disabled
                    if (isPaired) {
                        Platform.runLater(() -> {
                            if (cardIndex < cardComponents.size()) {
                                CardComponent card = cardComponents.get(cardIndex);

                                // Make sure paired cards are flipped
                                if (!card.isFlipped()) {
                                    card.flipCard();
                                }

                                LOGGER.info("Card " + cardIndex + " is paired, flipped and disabled");
                            }
                        });
                    }
                }

                LOGGER.info("Paired cards array updated: " + java.util.Arrays.toString(pairedCards));
            } catch (Exception e) {
                LOGGER.log(Level.SEVERE, "Error processing paired cards", e);
            }
        }
    }

    // This event happens when cards does not match and round turn is given to the opponent
    private void handleTurnSwitchEvent(JSONObject gameState) {
        LOGGER.info("Turn switch event: " + gameState);

        // First update UI for turn switch
        updateGameUI(gameState);

        // Store card indices before resetting
        final Integer firstCard = firstFlippedCardIndex;
        final Integer secondCard = secondFlippedCardIndex;

        // Reset state variables
        waitingForServerResponse = false;
        lastFlippedCardIndex = null;

        // Flip back cards with delay if they aren't paired
        if (firstCard != null && firstCard >= 0 && firstCard < pairedCards.length && !pairedCards[firstCard]) {
            LOGGER.info("Scheduling flip back for first card: " + firstCard);
            flipBackCardWithDelay(firstCard);
        }

        if (secondCard != null && secondCard >= 0 && secondCard < pairedCards.length && !pairedCards[secondCard]) {
            LOGGER.info("Scheduling flip back for second card: " + secondCard);
            flipBackCardWithDelay(secondCard);
        }

        // Only reset indices after scheduling the flips
        firstFlippedCardIndex = null;
        secondFlippedCardIndex = null;
    }

    private void flipBackCardWithDelay(int cardIndex) {
        if (cardIndex < 0 || cardIndex >= cardComponents.size()) {
            LOGGER.warning("Invalid card index: " + cardIndex);
            return;
        }

        CardComponent card = cardComponents.get(cardIndex);
        if (card.isFlipped()) {
            // Use JavaFX animation instead of Thread for UI operations
            javafx.animation.PauseTransition pause = new javafx.animation.PauseTransition(Duration.millis(1000));
            pause.setOnFinished(event -> {
                if (card.isFlipped() && !pairedCards[cardIndex]) {
                    card.flipCard();
                    LOGGER.info("Card " + cardIndex + " flipped back");
                }
            });
            pause.play();
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