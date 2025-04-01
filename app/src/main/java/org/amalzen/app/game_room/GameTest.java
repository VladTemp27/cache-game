package org.amalzen.app.game_room;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.Scanner;
import java.util.concurrent.CountDownLatch;
import java.util.logging.ConsoleHandler;
import java.util.logging.Level;
import java.util.logging.Logger;

public class GameTest {
    private static final Logger LOGGER = Logger.getLogger(GameTest.class.getName());
    private static boolean verbose = false;

    public static void main(String[] args) {
        // Setup logging
        configureLogging();

        // Parse command line arguments
        String gameId = args.length > 0 ? args[0] : "test-game";
        int playerId = args.length > 1 ? Integer.parseInt(args[1]) : 1;
        String username = args.length > 2 ? args[2] : "test-user";
        verbose = args.length > 3 && args[3].equalsIgnoreCase("verbose");

        LOGGER.info("Starting Game Room Client");
        LOGGER.info("Game ID: " + gameId);
        LOGGER.info("Player ID: " + playerId);

        // Create a latch to keep the main thread alive
        CountDownLatch exitLatch = new CountDownLatch(1);

        // Create and configure the game room model
        try (GameRoomModel gameRoom = new GameRoomModel(gameId, playerId, username)) {
            gameRoom.onConnected(() -> {
                        LOGGER.info("Successfully connected to game room server");
                        printHelp();
                    })
                    .onGameStateUpdate(response -> {
                        if (verbose) {
                            // In verbose mode, print full response
                            LOGGER.info("Game state update received:");
                            LOGGER.info(response.toString(2));
                        } else {
                            // In normal mode, print formatted game state
                            displayGameState(response);
                        }
                    })
                    .onConnectionClosed(() -> {
                        LOGGER.info("Connection to game room server closed");
                        exitLatch.countDown();
                    })
                    .onError(error -> {
                        LOGGER.log(Level.SEVERE, "Error in game room client", error);
                    });

            // Connect to the server
            LOGGER.info("Connecting to game room server...");
            gameRoom.connect().thenRun(() -> {
                LOGGER.info("Connected and ready to process commands");
            }).exceptionally(ex -> {
                LOGGER.log(Level.SEVERE, "Failed to connect", ex);
                exitLatch.countDown();
                return null;
            });

            // Process user input
            processUserCommands(gameRoom, exitLatch);

        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Unexpected error", e);
        }

        LOGGER.info("Game client terminated");
    }

    private static void displayGameState(JSONObject gameState) {
        clearConsole();

        if (gameState.has("status")) {
            String status = gameState.getString("status");
            System.out.println("Game Status: " + status);

            if (status.equals("match_end")) {
                if (gameState.has("winner")) {
                    int winner = gameState.getInt("winner");
                    if (winner == -1) {
                        System.out.println("Game is Tied");
                    } else if (gameState.has("usernames")) {
                        JSONArray usernames = gameState.getJSONArray("usernames");
                        System.out.println("Winner: " + usernames.getString(winner));
                    }
                }
                if (gameState.has("scores")) {
                    JSONArray scores = gameState.getJSONArray("scores");
                    System.out.println("Score: " + scores.getInt(0) + " - " + scores.getInt(1));
                }
            } else {
                System.out.println("Game Details:");
                if (gameState.has("cards")) System.out.println("Cards: " + gameState.get("cards"));
                if (gameState.has("paired")) System.out.println("Paired: " + gameState.get("paired"));
                if (gameState.has("yourScore")) System.out.println("Your Score: " + gameState.getInt("yourScore"));
                if (gameState.has("oppScore")) System.out.println("Opponent's Score: " + gameState.getInt("oppScore"));
                if (gameState.has("round")) System.out.println("Round: " + gameState.getInt("round"));
                if (gameState.has("whoseTurn")) System.out.println("Whose Turn: " + gameState.getString("whoseTurn"));
                if (gameState.has("timer")) System.out.println("Timer: " + gameState.getInt("timer"));
            }
        } else {
            // For messages without a status field
            if (gameState.has("type")) {
                System.out.println("Message Type: " + gameState.getString("type"));
                if (gameState.has("message")) {
                    System.out.println("Message: " + gameState.getString("message"));
                }
            }
        }

        System.out.print("> ");
    }

    private static void clearConsole() {
        System.out.print("\033[H\033[2J");
        System.out.flush();
    }

    private static void configureLogging() {
        Logger rootLogger = Logger.getLogger("");
        rootLogger.setLevel(Level.INFO);

        // Remove any existing handlers
        for (java.util.logging.Handler handler : rootLogger.getHandlers()) {
            rootLogger.removeHandler(handler);
        }

        // Add console handler with custom formatter to minimize noise
        ConsoleHandler handler = new ConsoleHandler();
        handler.setLevel(Level.INFO);
        rootLogger.addHandler(handler);

        // Suppress websocket verbose logging
        Logger.getLogger("org.amalzen.app.game_room.GameRoomModel").setLevel(Level.WARNING);
    }

    private static void printHelp() {
        System.out.println("\nAvailable commands:");
        System.out.println("  flip <index> - Flip a card at the specified index");
        System.out.println("  success - Send match success");
        System.out.println("  fail    - Send match failure");
        System.out.println("  quit    - Quit the game");
        System.out.println("  exit    - Exit the client");
        System.out.println("  help    - Show this help message");
        System.out.print("> ");
    }

    private static void processUserCommands(GameRoomModel gameRoom, CountDownLatch exitLatch) {
        Scanner scanner = new Scanner(System.in);

        Thread consoleThread = new Thread(() -> {
            try {
                while (true) {
                    String input = scanner.nextLine().trim();
                    String[] parts = input.split("\\s+");
                    String command = parts.length > 0 ? parts[0].toLowerCase() : "";

                    switch (command) {
                        case "flip":
                            if (parts.length > 1) {
                                try {
                                    int cardIndex = Integer.parseInt(parts[1]);
                                    gameRoom.sendFlip(cardIndex);
                                } catch (NumberFormatException nfe) {
                                    System.out.println("Invalid card index. Usage: flip <card index>");
                                    System.out.print("> ");
                                }
                            } else {
                                System.out.println("Missing card index. Usage: flip <card index>");
                                System.out.print("> ");
                            }
                            break;
                        case "success":
                            gameRoom.sendMatchSuccess();
                            break;
                        case "fail":
                            gameRoom.sendMatchFailure();
                            break;
                        case "quit":
                            gameRoom.sendQuit();
                            break;
                        case "exit":
                            gameRoom.sendQuit();
                            gameRoom.disconnect();
                            exitLatch.countDown();
                            return;
                        case "help":
                            printHelp();
                            break;
                        default:
                            System.out.println("Unknown command. Type 'help' for available commands.");
                            System.out.print("> ");
                            break;
                    }

                    if (!gameRoom.isConnected()) {
                        System.out.println("Connection lost, exiting command processor");
                        exitLatch.countDown();
                        return;
                    }
                }
            } catch (Exception e) {
                LOGGER.log(Level.SEVERE, "Error processing user input", e);
                exitLatch.countDown();
            }
        }, "UserInputThread");

        consoleThread.setDaemon(true);
        consoleThread.start();

        try {
            exitLatch.await();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            LOGGER.warning("Main thread interrupted");
        }
    }
}