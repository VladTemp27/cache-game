package org.amalzen.app.game_room;

import org.json.JSONObject;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.WebSocket;
import java.util.concurrent.*;
import java.util.function.Consumer;
import java.util.logging.Level;
import java.util.logging.Logger;

public class GameRoomClient implements AutoCloseable {
    private static final Logger LOGGER = Logger.getLogger(GameRoomClient.class.getName());
    private WebSocket webSocket;
    private final String serverUrl;
    private final String gameId;
    private final int playerId;
    private volatile boolean connected = false;
    private final ExecutorService callbackExecutor;
    private final ScheduledExecutorService reconnectExecutor;

    // Callbacks for game state updates
    private Consumer<JSONObject> onGameStateUpdate;
    private Runnable onConnectionClosed;
    private Consumer<Throwable> onError;
    private Runnable onConnected;

    // Reconnection configuration
    private boolean autoReconnect = true;
    private int maxReconnectAttempts = 5;
    private long reconnectDelayMs = 2000;
    private int reconnectAttempts = 0;

    public GameRoomClient(String gameId, int playerId) {
        this(gameId, playerId, "ws://localhost:8080/ws");
    }

    public GameRoomClient(String gameId, int playerId, String serverUrl) {
        this.gameId = gameId;
        this.playerId = playerId;
        this.serverUrl = serverUrl;
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
        String url = serverUrl + "?gameID=" + gameId + "&player=" + playerId;
        CompletableFuture<Void> connectionFuture = new CompletableFuture<>();

        try (HttpClient client = HttpClient.newHttpClient()) {
            CompletableFuture<WebSocket> ws = client.newWebSocketBuilder()
                    .buildAsync(URI.create(url), new WebSocketListener());

            ws.thenAccept(websocket -> {
                webSocket = websocket;
                connected = true;
                reconnectAttempts = 0;
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

    public GameRoomClient sendMatchSuccess() {
        sendAction("move", true);
        return this;
    }

    public GameRoomClient sendMatchFailure() {
        sendAction("move", false);
        return this;
    }

    public GameRoomClient sendQuit() {
        sendAction("quit", false);
        return this;
    }

    @Override
    public void close() {
        disconnect();
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

    public void disconnect() {
        if (webSocket != null && connected) {
            try {
                webSocket.sendClose(WebSocket.NORMAL_CLOSURE, "Client disconnecting");
            } catch (Exception e) {
                LOGGER.log(Level.WARNING, "Error closing WebSocket", e);
            } finally {
                connected = false;
            }
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

    // Fluent API for callback setters
    public GameRoomClient onGameStateUpdate(Consumer<JSONObject> handler) {
        this.onGameStateUpdate = handler;
        return this;
    }

    public GameRoomClient onConnectionClosed(Runnable handler) {
        this.onConnectionClosed = handler;
        return this;
    }

    public GameRoomClient onError(Consumer<Throwable> handler) {
        this.onError = handler;
        return this;
    }

    public GameRoomClient onConnected(Runnable handler) {
        this.onConnected = handler;
        return this;
    }

    public GameRoomClient withAutoReconnect(boolean autoReconnect, int maxAttempts, long delayMs) {
        this.autoReconnect = autoReconnect;
        this.maxReconnectAttempts = maxAttempts;
        this.reconnectDelayMs = delayMs;
        return this;
    }

    public boolean isConnected() {
        return connected;
    }

    private void runCallback(Runnable callback) {
        callbackExecutor.execute(callback);
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

                runCallback(() -> {
                    try {
                        JSONObject jsonMessage = new JSONObject(completeMessage);
                        if (onGameStateUpdate != null) {
                            onGameStateUpdate.accept(jsonMessage);
                        }
                    } catch (Exception e) {
                        if (onError != null) {
                            onError.accept(e);
                        }
                    }
                });
            }

            return WebSocket.Listener.super.onText(webSocket, data, last);
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
            runCallback(() -> {
                if (onError != null) {
                    onError.accept(error);
                }
            });
            WebSocket.Listener.super.onError(webSocket, error);
        }
    }
}