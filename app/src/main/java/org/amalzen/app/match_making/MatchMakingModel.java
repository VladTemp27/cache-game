package org.amalzen.app.match_making;

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

public class MatchMakingModel implements AutoCloseable {
    private static final Logger LOGGER = Logger.getLogger(MatchMakingModel.class.getName());
    private static final String SERVER_URL = APIs.MM_URL.getValue();
    private static final String username = Main.username;
    private static final String token = Main.sessionId;

    // WebSocket and connection state
    private WebSocket webSocket;
    private volatile boolean connected = false;

    // Thread management
    private final ExecutorService callbackExecutor;
    private final ScheduledExecutorService reconnectExecutor;

    // Callbacks
    private Runnable onConnected;
    private Runnable onQueueSuccess;
    private Consumer<JSONObject> onMatchFound;
    private Runnable onConnectionClosed;
    private Consumer<String> onQueueTimeout;
    private Consumer<Throwable> onError;

    // Reconnection configuration
    private boolean autoReconnect = true;
    private int maxReconnectAttempts = 5;
    private long reconnectDelayMs = 2000;
    private int reconnectAttempts = 0;

    private volatile boolean shuttingDown = false;

    // Player data
    private int playerScore = 300; // Default player score

    public MatchMakingModel() {
        this.callbackExecutor = Executors.newSingleThreadExecutor(r -> {
            Thread t = new Thread(r, "MatchMakingClient-Callback");
            t.setDaemon(true);
            return t;
        });
        this.reconnectExecutor = Executors.newSingleThreadScheduledExecutor(r -> {
            Thread t = new Thread(r, "MatchMakingClient-Reconnect");
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

            // Add logging for debugging
            LOGGER.info("Attempting to connect to " + SERVER_URL);
            LOGGER.info("Authentication: username=" + username + ", token present=" + (token != null));

            CompletableFuture<WebSocket> ws = client.newWebSocketBuilder()
                    .header("User-Agent", "JavaFX-Client")  // Add a custom header
                    .connectTimeout(Duration.ofSeconds(10))
                    .buildAsync(URI.create(SERVER_URL), new WebSocketListener());

            ws.thenAccept(websocket -> {
                webSocket = websocket;
                connected = true;
                reconnectAttempts = 0;
                LOGGER.info("WebSocket connection established successfully");

                // Send a ping message to verify connection
                JSONObject pingMessage = new JSONObject();
                pingMessage.put("type", "ping");
                webSocket.sendText(pingMessage.toString(), true);

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

    public void enterQueue() {
        if (!connected) {
            LOGGER.warning("Cannot enter queue: not connected");
            return;
        }

        if (username == null || token == null) {
            LOGGER.warning("Cannot enter queue: missing credentials");
            if (onError != null) {
                onError.accept(new IllegalStateException("Missing authentication data"));
            }
            return;
        }

        JSONObject message = new JSONObject();
        message.put("type", "queue");
        message.put("username", username);
        message.put("token", token);
        message.put("score", playerScore);

        System.out.println("Sending message: " + message.toString());

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

    public void cancelQueue() {
        if (!connected) {
            LOGGER.warning("Cannot cancel queue: not connected");
            return;
        }

        JSONObject message = new JSONObject();
        message.put("type", "cancel");
        message.put("username", username);
        message.put("token", token);
        message.put("score", playerScore);

        System.out.println("Sending message: " + message.toString());

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
    public MatchMakingModel onConnected(Runnable handler) {
        this.onConnected = handler;
        return this;
    }

    public MatchMakingModel onQueueSuccess(Runnable handler) {
        this.onQueueSuccess = handler;
        return this;
    }

    public MatchMakingModel onMatchFound(Consumer<JSONObject> handler) {
        this.onMatchFound = handler;
        return this;
    }
    public MatchMakingModel onQueueTimeout(Consumer<String> handler) {
        this.onQueueTimeout = handler;
        return this;
    }

    public MatchMakingModel onConnectionClosed(Runnable handler) {
        this.onConnectionClosed = handler;
        return this;
    }

    public MatchMakingModel onError(Consumer<Throwable> handler) {
        this.onError = handler;
        return this;
    }

    public boolean isConnected() {
        return connected;
    }

    public void setPlayerScore(int score) {
        this.playerScore = score;
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

    // WebSocket listener implementation
    private class WebSocketListener implements WebSocket.Listener {
        private final StringBuilder messageBuilder = new StringBuilder();

        @Override
        public CompletionStage<?> onText(WebSocket webSocket, CharSequence data, boolean last) {
            messageBuilder.append(data);

            if (last) {
                final String completeMessage = messageBuilder.toString();
                messageBuilder.setLength(0);

                runCallback(() -> {processServerResponse(completeMessage);});

                webSocket.request(1);
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
        public void onOpen(WebSocket webSocket) {
            LOGGER.info("WebSocket connection opened");
            webSocket.request(1);
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

    public void processServerResponse(String jsonResponse) {
        JSONObject response;
        try {
            response = new JSONObject(jsonResponse);
            LOGGER.info("Received message: " + response.toString());
        } catch(JSONException jsonException){
            LOGGER.warning("Error parsing JSON: " + jsonException.getMessage());
            return;
        }

        LOGGER.info("PARSING RESPONSE" + response.toString());
        switch (response.getString("type")){
            case "welcome":
                LOGGER.info("Connected to server: " + response.optString("message"));
                if (onConnected != null) {
                    onConnected.run();
                }
                break;
            case "queue_success":
                if (onQueueSuccess != null) {
                    onQueueSuccess.run();
                }
                break;
            case "match_found":
                if (onMatchFound != null) {
                    onMatchFound.accept(response);
                }
                break;
            case "connection_closing":
                LOGGER.info("Server is closing connection: " + response.optString("message"));
                break;
            case "error":
                if (onError != null) {
                    onError.accept(new RuntimeException(response.optString("message", "Unknown error")));
                }
                break;
            case "queue_timeout":
                String timeoutMessage = response.optString("message", "Queue timed out");
                LOGGER.info("Queue timeout: " + timeoutMessage);
                if (onQueueTimeout != null) {
                    onQueueTimeout.accept(timeoutMessage);
                } else {
                    // Default behavior if no handler is registered
                    cancelQueue();
                    if (onError != null) {
                        onError.accept(new RuntimeException("Queue timeout: " + timeoutMessage));
                    }
                }
                break;
            default:
                LOGGER.warning("Unknown message type: " + response.getString("type"));
        }
    }
}