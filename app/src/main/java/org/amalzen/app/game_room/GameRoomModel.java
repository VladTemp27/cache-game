package org.amalzen.app.game_room;

import org.amalzen.app.APIs;
import org.amalzen.app.Main;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.WebSocket;
import java.time.Duration;
import java.util.concurrent.*;
import java.util.function.Consumer;
import java.util.logging.Level;
import java.util.logging.Logger;

public class GameRoomModel implements AutoCloseable {
    private static final Logger LOGGER = Logger.getLogger(GameRoomModel.class.getName());
    private final String SERVER_URL = APIs.GR_URL.getValue();
    private final String gameId;
    private final int playerId;
    private String username = Main.username;
    private final String token = Main.sessionId;

    // WebSocket and connection state
    private WebSocket webSocket;
    private volatile boolean connected = false;

    // Thread management
    private final ExecutorService callbackExecutor;
    private final ScheduledExecutorService reconnectExecutor;
    private volatile boolean shuttingDown = false;

    // Game state
    private String[] cards;
    private boolean[] paired;
    private int flippedCardIndex = -1;
    private String opponentName;
    private int yourScore = 0;
    private int opponentScore = 0;
    private int round = 1;
    private String whoseTurn;
    private int timeRemaining;
    private String gameStatus = "waiting";

    // Callbacks
    private Consumer<String[]> onGameReady;
    private Consumer<Integer> onCardFlipped;
    private Consumer<boolean[]> onCardsMatched;
    private Consumer<String> onTurnSwitch;
    private Consumer<JSONObject> onGameEnd;
    private Runnable onConnectionClosed;
    private Consumer<Throwable> onError;
    private Runnable onConnected;
    private Consumer<Integer> onTimerUpdate;

    // Reconnection configuration
    private boolean autoReconnect = true;
    private int maxReconnectAttempts = 5;
    private long reconnectDelayMs = 2000;
    private int reconnectAttempts = 0;

    public GameRoomModel(String gameId, int playerId, String username) {
        this.gameId = gameId;
        this.playerId = playerId;
        this.username = username;
        this.callbackExecutor = Executors.newSingleThreadExecutor(r -> {
            Thread t = new Thread(r, "GameRoomClient-Callback");
            t.setDaemon(true);
            return t;
        });
        this.reconnectExecutor = Executors.newSingleThreadScheduledExecutor(r -> {
            Thread t = new Thread(r, "GameRoomClient-Reconnect");
            t.setDaemon(true);
            return t;
        });
    }

    public CompletableFuture<Void> connect() {
        CompletableFuture<Void> connectionFuture = new CompletableFuture<>();

        try {
            HttpClient client = HttpClient.newBuilder()
                    .connectTimeout(Duration.ofSeconds(10))
                    .build();

            String url = SERVER_URL + "?gameID=" + gameId + "&player=" + playerId + "&username=" + username;
            LOGGER.info("Attempting to connect to " + url);
            LOGGER.info("Authentication: username=" + username + ", token present=" + (token != null));

            CompletableFuture<WebSocket> ws = client.newWebSocketBuilder()
                    .header("User-Agent", "JavaFX-Client")
                    .connectTimeout(Duration.ofSeconds(10))
                    .buildAsync(URI.create(url), new WebSocketListener());

            ws.thenAccept(websocket -> {
                webSocket = websocket;
                connected = true;
                reconnectAttempts = 0;
                LOGGER.info("WebSocket connection established successfully");

                runCallback(() -> {
                    if (onConnected != null) {
                        onConnected.run();
                    }
                });
                connectionFuture.complete(null);
            }).exceptionally(e -> {
                handleConnectionFailure(e, connectionFuture);
                return null;
            });
        } catch (Exception e) {
            handleConnectionFailure(e, connectionFuture);
        }

        return connectionFuture;
    }

    private void handleConnectionFailure(Throwable e, CompletableFuture<Void> connectionFuture) {
        runCallback(() -> {
            if (onError != null) {
                onError.accept(e);
            }
        });

        if (autoReconnect && reconnectAttempts < maxReconnectAttempts) {
            reconnectAttempts++;
            LOGGER.log(Level.INFO, "Connection attempt failed. Scheduling reconnect {0}/{1} in {2}ms",
                    new Object[]{reconnectAttempts, maxReconnectAttempts, reconnectDelayMs});

            reconnectExecutor.schedule(() -> {
                connect();
            }, reconnectDelayMs, TimeUnit.MILLISECONDS);
        } else {
            connectionFuture.completeExceptionally(e);
        }
    }

    public void sendFlip(int cardIndex) {
        if (!connected) {
            LOGGER.warning("Cannot send flip: not connected");
            return;
        }

        JSONObject message = new JSONObject();
        message.put("action", "flip");
        message.put("cardIndex", cardIndex);

        LOGGER.info("Sending flip action for card index: " + cardIndex);
        try {
            webSocket.sendText(message.toString(), true);
        } catch (Exception e) {
            runCallback(() -> {
                if (onError != null) {
                    onError.accept(e);
                }
            });
        }

        // Track the flipped card locally
        if (flippedCardIndex == -1) {
            flippedCardIndex = cardIndex;
        } else {
            // If we're flipping second card, notify listener
            runCallback(() -> {
                if (onCardFlipped != null) {
                    onCardFlipped.accept(cardIndex);
                }
            });
        }
    }

    public void sendMatchSuccess() {
        sendMove(true);
    }

    public void sendMatchFailure() {
        sendMove(false);
    }

    private void sendMove(boolean matched) {
        if (!connected) {
            LOGGER.warning("Cannot send move: not connected");
            return;
        }

        JSONObject message = new JSONObject();
        message.put("action", "move");
        message.put("matched", matched);

        LOGGER.info("Sending move action with matched=" + matched);
        try {
            webSocket.sendText(message.toString(), true);
        } catch (Exception e) {
            runCallback(() -> {
                if (onError != null) {
                    onError.accept(e);
                }
            });
        }

        // Reset the flipped card state after sending move
        flippedCardIndex = -1;
    }

    public void sendQuit() {
        if (!connected) {
            LOGGER.warning("Cannot send quit: not connected");
            return;
        }

        JSONObject message = new JSONObject();
        message.put("action", "quit");

        LOGGER.info("Sending quit action");
        try {
            webSocket.sendText(message.toString(), true);
        } catch (Exception e) {
            runCallback(() -> {
                if (onError != null) {
                    onError.accept(e);
                }
            });
        }
    }

    public CompletableFuture<Void> disconnect() {
        CompletableFuture<Void> closeFuture = new CompletableFuture<>();

        if (webSocket != null && connected) {
            try {
                // Set a timeout to ensure we don't block forever
                CompletableFuture.runAsync(() -> {
                    try {
                        Thread.sleep(1000); // Give 1 second for clean close
                        if (!closeFuture.isDone()) {
                            closeFuture.complete(null);
                        }
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                    }
                });

                // Request closing with proper handshake
                webSocket.sendClose(WebSocket.NORMAL_CLOSURE, "Client disconnecting")
                        .thenRun(() -> closeFuture.complete(null));
            } catch (Exception e) {
                LOGGER.log(Level.WARNING, "Error closing WebSocket", e);
                closeFuture.complete(null);
            } finally {
                connected = false;
            }
        } else {
            closeFuture.complete(null);
        }

        return closeFuture;
    }

    @Override
    public void close() {
        shuttingDown = true;
        CompletableFuture<Void> disconnectFuture = disconnect();

        try {
            disconnectFuture.get(3, TimeUnit.SECONDS);
        } catch (Exception e) {
            LOGGER.log(Level.WARNING, "Error waiting for WebSocket to disconnect", e);
        }

        callbackExecutor.shutdown();
        reconnectExecutor.shutdown();

        try {
            if (!callbackExecutor.awaitTermination(5, TimeUnit.SECONDS)) {
                callbackExecutor.shutdownNow();
            }
            if (!reconnectExecutor.awaitTermination(5, TimeUnit.SECONDS)) {
                reconnectExecutor.shutdownNow();
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            LOGGER.log(Level.WARNING, "Interrupted while waiting for executor shutdown", e);
        }
    }

    // Callback setters
    public GameRoomModel onGameReady(Consumer<String[]> handler) {
        this.onGameReady = handler;
        return this;
    }

    public GameRoomModel onCardFlipped(Consumer<Integer> handler) {
        this.onCardFlipped = handler;
        return this;
    }

    public GameRoomModel onCardsMatched(Consumer<boolean[]> handler) {
        this.onCardsMatched = handler;
        return this;
    }

    public GameRoomModel onTurnSwitch(Consumer<String> handler) {
        this.onTurnSwitch = handler;
        return this;
    }

    public GameRoomModel onGameEnd(Consumer<JSONObject> handler) {
        this.onGameEnd = handler;
        return this;
    }

    public GameRoomModel onConnectionClosed(Runnable handler) {
        this.onConnectionClosed = handler;
        return this;
    }

    public GameRoomModel onError(Consumer<Throwable> handler) {
        this.onError = handler;
        return this;
    }

    public GameRoomModel onConnected(Runnable handler) {
        this.onConnected = handler;
        return this;
    }

    public GameRoomModel onTimerUpdate(Consumer<Integer> handler) {
        this.onTimerUpdate = handler;
        return this;
    }

    public GameRoomModel withAutoReconnect(boolean autoReconnect, int maxAttempts, long delayMs) {
        this.autoReconnect = autoReconnect;
        this.maxReconnectAttempts = maxAttempts;
        this.reconnectDelayMs = delayMs;
        return this;
    }

    public boolean isConnected() {
        return connected;
    }

    // Game state getters
    public String getOpponentName() {
        return opponentName;
    }

    public int getYourScore() {
        return yourScore;
    }

    public int getOpponentScore() {
        return opponentScore;
    }

    public String getWhoseTurn() {
        return whoseTurn;
    }

    public boolean isYourTurn() {
        return username.equals(whoseTurn);
    }

    public int getTimeRemaining() {
        return timeRemaining;
    }

    public String getGameStatus() {
        return gameStatus;
    }

    private void runCallback(Runnable callback) {
        // Skip if shutting down or already shutdown to prevent RejectedExecutionException
        if (shuttingDown || callbackExecutor.isShutdown()) {
            LOGGER.fine("Skipping callback execution during shutdown");
            return;
        }

        try {
            callbackExecutor.execute(callback);
        } catch (RejectedExecutionException e) {
            LOGGER.log(Level.WARNING, "Callback rejected (executor likely shutdown)", e);
        }
    }

    private void processServerResponse(String jsonResponse) {
        JSONObject response;
        try {
            response = new JSONObject(jsonResponse);
            // Only log detailed message in verbose mode
            if (LOGGER.isLoggable(Level.FINE)) {
                LOGGER.fine("Received message: " + response.toString(2));
            } else {
                // Log a brief message summary for regular level
                if (response.has("event")) {
                    LOGGER.info("Received event: " + response.getString("event"));
                }
            }
        } catch (JSONException jsonException) {
            LOGGER.warning("Error parsing JSON: " + jsonException.getMessage());
            return;
        }

        // Handle events based on event type
        if (response.has("event")) {
            String eventType = response.getString("event");

            switch (eventType) {
                case "game_ready":
                    handleGameReadyEvent(response);
                    break;
                case "players_ready":
                    handlePlayersReadyEvent(response);
                    break;
                case "card_flip":
                    handCardFlipEvent(response);
                    break;
                case "cards_matched":
                    handleCardsMatchedEvent(response);
                    break;
                case "turn_switch":
                    handleTurnSwitchEvent(response);
                    break;
                case "game_end":
                    handleGameEndEvent(response);
                    break;
                default:
                    LOGGER.warning("Unknown event type: " + eventType);
                    break;
            }
        }
    }

    private void handCardFlipEvent(JSONObject response) {
        LOGGER.fine("Handling card flipped event: " + response.toString(2));
        int flippedIndex = response.getInt("cardIndex");
        runCallback(() -> {
            if (onCardFlipped != null) {
                onCardFlipped.accept(flippedIndex);
            }
        });
    }

    private void handleGameReadyEvent(JSONObject response) {
        LOGGER.info("Game is ready!");
        gameStatus = "ready";

        // Extract card data
        JSONArray cardsArray = response.getJSONArray("cards");
        cards = new String[cardsArray.length()];
        paired = new boolean[cardsArray.length()];

        for (int i = 0; i < cardsArray.length(); i++) {
            cards[i] = cardsArray.getString(i);
        }

        opponentName = response.getString("opponentName");
        timeRemaining = response.getInt("timeDuration");

        // Notify listeners
        runCallback(() -> {
            if (onGameReady != null) {
                onGameReady.accept(cards);
            }
            if (onTimerUpdate != null) {
                onTimerUpdate.accept(timeRemaining);
            }
        });
    }

    private void handlePlayersReadyEvent(JSONObject response) {
        LOGGER.info("All players are ready!");
        gameStatus = "playing";

        yourScore = response.getInt("yourScore");
        opponentScore = response.getInt("oppScore");
        whoseTurn = response.getString("whoseTurn");

        runCallback(() -> {
            if (onTurnSwitch != null) {
                onTurnSwitch.accept(whoseTurn);
            }
        });
    }

    private void handleCardsMatchedEvent(JSONObject response) {
        LOGGER.info("Cards matched!");

        yourScore = response.getInt("yourScore");
        opponentScore = response.getInt("oppScore");
        whoseTurn = response.getString("whoseTurn");

        // Update paired cards
        JSONArray pairedArray = response.getJSONArray("paired");
        for (int i = 0; i < pairedArray.length(); i++) {
            paired[i] = pairedArray.getBoolean(i);
        }

        runCallback(() -> {
            if (onCardsMatched != null) {
                onCardsMatched.accept(paired);
            }
            if (onTurnSwitch != null) {
                onTurnSwitch.accept(whoseTurn);
            }
        });
    }

    private void handleTurnSwitchEvent(JSONObject response) {
        LOGGER.info("Turn switched!");

        round = response.getInt("round");
        whoseTurn = response.getString("whoseTurn");
        flippedCardIndex = -1; // Reset flipped card on turn switch

        runCallback(() -> {
            if (onTurnSwitch != null) {
                onTurnSwitch.accept(whoseTurn);
            }
        });
    }

    private void handleGameEndEvent(JSONObject response) {
        LOGGER.info("Game has ended!");
        gameStatus = "ended";

        runCallback(() -> {
            if (onGameEnd != null) {
                onGameEnd.accept(response);
            }
        });
    }

    public GameRoomModel onGameStateUpdate(Consumer<JSONObject> handler) {
        return this
                .onGameReady(cards -> {
                    JSONObject state = new JSONObject();
                    state.put("event", "game_ready");
                    state.put("cards", cards);
                    state.put("opponentName", opponentName);
                    state.put("yourName", username);
                    state.put("timeDuration", timeRemaining);
                    handler.accept(state);
                })
                .onCardFlipped(cardIndex -> {
                    JSONObject state = new JSONObject();
                    state.put("event", "card_flip");
                    state.put("flipped", cardIndex);
                    handler.accept(state);
                })
                .onCardsMatched(pairedCards -> {
                    JSONObject state = new JSONObject();
                    state.put("event", "cards_matched");
                    state.put("yourScore", yourScore);
                    state.put("oppScore", opponentScore);
                    state.put("paired", pairedCards);
                    state.put("whoseTurn", whoseTurn);
                    handler.accept(state);
                })
                .onTurnSwitch(turn -> {
                    JSONObject state = new JSONObject();
                    state.put("event", "turn_switch");
                    state.put("round", round);
                    state.put("whoseTurn", turn);
                    handler.accept(state);
                })
                .onGameEnd(state -> handler.accept(state))
                .onTimerUpdate(time -> {
                    JSONObject state = new JSONObject();
                    state.put("event", "timer_update");
                    state.put("timer", time);
                    handler.accept(state);
                });
    }

    public void startClientSideTimer() {
        if (timeRemaining <= 0) return;

        ScheduledExecutorService timerExecutor = Executors.newSingleThreadScheduledExecutor(r -> {
            Thread t = new Thread(r, "GameRoomClient-Timer");
            t.setDaemon(true);
            return t;
        });

        timerExecutor.scheduleAtFixedRate(() -> {
            if (connected && timeRemaining > 0) {
                timeRemaining--;
                runCallback(() -> {
                    if (onTimerUpdate != null) {
                        onTimerUpdate.accept(timeRemaining);
                    }
                });
            } else {
                timerExecutor.shutdown();
            }
        }, 0, 1, TimeUnit.SECONDS);
    }

    // WebSocket listener implementation
    private class WebSocketListener implements WebSocket.Listener {
        private final StringBuilder messageBuilder = new StringBuilder();

        @Override
        public CompletionStage<?> onText(WebSocket webSocket, CharSequence data, boolean last) {
            messageBuilder.append(data);

            if (last) {
                final String completeMessage = messageBuilder.toString();
                messageBuilder.setLength(0);  // Clear the buffer

                runCallback(() -> processServerResponse(completeMessage));
                webSocket.request(1);
            }

            return WebSocket.Listener.super.onText(webSocket, data, last);
        }

        @Override
        public void onOpen(WebSocket webSocket) {
            LOGGER.info("WebSocket connection opened");
            webSocket.request(1);
        }

        @Override
        public CompletionStage<?> onClose(WebSocket webSocket, int statusCode, String reason) {
            connected = false;

            runCallback(() -> {
                if (onConnectionClosed != null) {
                    onConnectionClosed.run();
                }
            });

            // Attempt reconnection if needed
            if (autoReconnect && statusCode != WebSocket.NORMAL_CLOSURE && reconnectAttempts < maxReconnectAttempts) {
                reconnectAttempts++;
                LOGGER.log(Level.INFO, "Connection closed. Scheduling reconnect {0}/{1} in {2}ms",
                        new Object[]{reconnectAttempts, maxReconnectAttempts, reconnectDelayMs});

                reconnectExecutor.schedule(() -> {
                    connect();
                }, reconnectDelayMs, TimeUnit.MILLISECONDS);
            }

            return WebSocket.Listener.super.onClose(webSocket, statusCode, reason);
        }

        @Override
        public void onError(WebSocket webSocket, Throwable error) {
            LOGGER.log(Level.SEVERE, "WebSocket error occurred", error);
            runCallback(() -> {
                if (onError != null) {
                    onError.accept(error);
                }
            });
            WebSocket.Listener.super.onError(webSocket, error);
        }
    }
}