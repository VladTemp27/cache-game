package org.amalzen.app.game_room;

import org.json.JSONObject;

import java.util.Scanner;
import java.util.concurrent.CountDownLatch;
import java.util.logging.Level;
import java.util.logging.Logger;

public class ClientTest {
    private static final Logger LOGGER = Logger.getLogger(ClientTest.class.getName());
    private static volatile boolean running = true;

    public static void main(String[] args) {
        // Parse command line arguments or use defaults
        String gameId = args.length > 0 ? args[0] : "game1";
        int playerId = args.length > 1 ? Integer.parseInt(args[1]) : 1;
        String serverUrl = args.length > 2 ? args[2] : "ws://localhost:8080/ws";

        CountDownLatch exitLatch = new CountDownLatch(1);

        try (GameRoomModel client = new GameRoomModel(gameId, playerId, serverUrl);
             Scanner scanner = new Scanner(System.in)) {

            // Configure client callbacks
            client.onConnected(() -> {
                    LOGGER.info("Connected to game server!");
                    System.out.println("Connected to game server. Type commands to play:");
                    System.out.println("- 's' to send match success");
                    System.out.println("- 'f' to send match failure");
                    System.out.println("- 'q' to quit");
                })
                .onConnectionClosed(() -> {
                    LOGGER.info("Disconnected from game server");
                    System.out.println("Disconnected from server");
                    if (!running) {
                        exitLatch.countDown();
                    }
                })
                .onError(error -> {
                    LOGGER.log(Level.WARNING, "Error occurred", error);
                    System.out.println("Error: " + error.getMessage());
                })
                .onGameStateUpdate(gameState -> {
                    System.out.println("Game state update: " + formatGameState(gameState));
                })
                .withAutoReconnect(true, 5, 2000);

            // Connect to the server
            System.out.println("Connecting to game server...");
            client.connect().thenRun(() -> {
                System.out.println("Connected!");
            }).exceptionally(e -> {
                System.err.println("Failed to connect: " + e.getMessage());
                exitLatch.countDown();
                return null;
            });

            // Add shutdown hook for proper cleanup
            Runtime.getRuntime().addShutdownHook(new Thread(() -> {
                running = false;
                System.out.println("Shutting down...");
                client.disconnect();
            }));

            // Process user input
            while (running) {
                String input = scanner.nextLine().trim().toLowerCase();
                switch (input) {
                    case "s":
                        System.out.println("Sending match success");
                        client.sendMatchSuccess();
                        break;
                    case "f":
                        System.out.println("Sending match failure");
                        client.sendMatchFailure();
                        break;
                    case "q":
                        System.out.println("Quitting game");
                        running = false;
                        client.sendQuit();
                        client.disconnect();
                        exitLatch.countDown();
                        break;
                    default:
                        System.out.println("Unknown command: " + input);
                        break;
                }
            }

            // Wait for disconnection before exiting
            exitLatch.await();

        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Unhandled exception", e);
            System.err.println("Error: " + e.getMessage());
        }
    }

    private static String formatGameState(JSONObject gameState) {
        StringBuilder sb = new StringBuilder();

        if (gameState.has("type")) {
            String type = gameState.getString("type");
            sb.append("Type: ").append(type).append(", ");

            if (type.equals("state") && gameState.has("state")) {
                sb.append("Game State: ").append(gameState.getString("state"));
            } else if (type.equals("move") && gameState.has("result")) {
                sb.append("Move Result: ").append(gameState.getBoolean("result"));
            } else if (type.equals("error")) {
                sb.append("Error: ").append(gameState.optString("message", "Unknown error"));
            }
        } else {
            sb.append(gameState);
        }

        return sb.toString();
    }
}