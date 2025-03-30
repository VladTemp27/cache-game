package org.amalzen.app.game_room;

import java.util.Scanner;
import java.util.concurrent.CountDownLatch;
import java.util.logging.ConsoleHandler;
import java.util.logging.Level;
import java.util.logging.Logger;

public class GameTest {
    private static final Logger LOGGER = Logger.getLogger(GameTest.class.getName());

    public static void main(String[] args) {
        // Setup logging
        configureLogging();

        // Parse command line arguments
        String gameId = args.length > 0 ? args[0] : "test-game";
        int playerId = args.length > 1 ? Integer.parseInt(args[1]) : 1;
        String username = args.length > 2 ? args[2] : "test-user";

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
//                        LOGGER.info("Game state update received:");
//                        LOGGER.info(response.toString(2));
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

    private static void configureLogging() {
        Logger rootLogger = Logger.getLogger("");
        rootLogger.setLevel(Level.INFO);
        ConsoleHandler handler = new ConsoleHandler();
        handler.setLevel(Level.INFO);
        rootLogger.addHandler(handler);
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
//                                    LOGGER.info("Sending flip action for card index: " + cardIndex);
                                    gameRoom.sendFlip(cardIndex);
                                } catch (NumberFormatException nfe) {
                                    System.out.println("Invalid card index. Usage: flip <card index>");
                                }
                            } else {
                                System.out.println("Missing card index. Usage: flip <card index>");
                            }
                            break;
                        case "success":
                            LOGGER.info("Sending match success");
                            gameRoom.sendMatchSuccess();
                            break;
                        case "fail":
                            LOGGER.info("Sending match failure");
                            gameRoom.sendMatchFailure();
                            break;
                        case "quit":
                            LOGGER.info("Sending quit command");
                            gameRoom.sendQuit();
                            break;
                        case "exit":
                            LOGGER.info("Exiting client");
                            gameRoom.sendQuit();
                            gameRoom.disconnect();
                            exitLatch.countDown();
                            return;
                        case "help":
                            printHelp();
                            break;
                        default:
                            System.out.println("Unknown command. Type 'help' for available commands.");
                            break;
                    }

                    if (!gameRoom.isConnected()) {
                        LOGGER.info("Connection lost, exiting command processor");
                        exitLatch.countDown();
                        return;
                    }

                    System.out.print("> ");
                }
            } catch (Exception e) {
                LOGGER.log(Level.SEVERE, "Error processing user input", e);
                exitLatch.countDown();
            }
        }, "UserInputThread");

        consoleThread.setDaemon(true);
        consoleThread.start();

        try {
            // Wait for signal to exit
            exitLatch.await();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            LOGGER.warning("Main thread interrupted");
        }
    }
}