package org.amalzen.app.game_room;

import org.amalzen.app.APIs;
import org.amalzen.app.Main;
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

    // Callbacks
    private Consumer<JSONObject> onGameStateUpdate;
    private Runnable onConnectionClosed;
    private Consumer<Throwable> onError;
    private Runnable onConnected;

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
            // Create a new HttpClient for each connection attempt
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

    public void sendMatchSuccess() {
        sendAction("move", true);
    }

    public void sendMatchFailure() {
        sendAction("move", false);
    }

    public void sendQuit() {
        sendAction("quit", false);
    }
    public void sendFlip(int cardIndex) {
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
    }

    private void sendAction(String action, boolean matched) {
        if (!connected) {
            LOGGER.warning("Cannot send action: not connected");
            return;
        }

        JSONObject message = new JSONObject();
        message.put("action", action);
        if (action.equals("move")) {
            message.put("matched", matched);
        }

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

    // Fluent API for callback setters
    public GameRoomModel onGameStateUpdate(Consumer<JSONObject> handler) {
        this.onGameStateUpdate = handler;
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

    public GameRoomModel withAutoReconnect(boolean autoReconnect, int maxAttempts, long delayMs) {
        this.autoReconnect = autoReconnect;
        this.maxReconnectAttempts = maxAttempts;
        this.reconnectDelayMs = delayMs;
        return this;
    }

    public boolean isConnected() {
        return connected;
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
//            LOGGER.info("Received message: " + response.toString());
        } catch (JSONException jsonException) {
            LOGGER.warning("Error parsing JSON: " + jsonException.getMessage());
            return;
        }

        if (onGameStateUpdate != null) {
            onGameStateUpdate.accept(response);
        }

        // Handle specific server response types if needed
        if (response.has("type")) {
            String type = response.getString("type");
            switch (type) {
                case "error":
                    if (onError != null) {
                        onError.accept(new RuntimeException(response.optString("message", "Unknown error")));
                    }
                    break;
                case "connection_closing":
                    LOGGER.info("Server is closing connection: " + response.optString("message"));
                    break;
                default:
                    // All other messages handled by the general onGameStateUpdate
                    break;
            }
        }
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