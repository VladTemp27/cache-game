package org.amalzen.app.match_making;

import javafx.animation.RotateTransition;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.image.ImageView;
import javafx.scene.layout.AnchorPane;
import javafx.util.Duration;
import org.amalzen.app.Main;
import org.amalzen.app.ResourcePath;
import org.json.JSONObject;

import java.util.function.Consumer;
import java.util.logging.Level;
import java.util.logging.Logger;

public class MatchmakingController {
    private static final Logger LOGGER = Logger.getLogger(MatchmakingController.class.getName());

    @FXML
    private Button cancelButton;
    @FXML
    private ImageView loadingBall;
    @FXML
    public AnchorPane rootPane;

    private MatchMakingModel matchmakingModel;
    private boolean matchFound = false;

    @FXML
    private void initialize() {
        startImageSpin();
        setupMatchmaking();

        // Register cleanup handler on scene changes
        rootPane.sceneProperty().addListener((obs, oldScene, newScene) -> {
            if (oldScene != null && newScene == null) {
                // Scene is being detached, perform cleanup
                cleanup();
            }
        });

        cancelButton.setOnMouseClicked(event -> {
            matchmakingModel.cancelQueue();
            returnToMainMenu();
        });
    }

    private void setupMatchmaking() {
        matchmakingModel = new MatchMakingModel();

        // Default score or difficulty chosen by player
        // TODO: This must be taken from the player's metadata not statically set
        int playerScore = 300;
        matchmakingModel.setPlayerScore(playerScore);

        // Configure callbacks
        // Enter matchmaking queue with player's score
        matchmakingModel.onConnected(handleOnConnected())
                .onQueueSuccess(handleOnQueueSuccess())
                .onMatchFound(handleOnMatchFound())
                .onQueueTimeout(handleOnQueueTimeout())
                .onConnectionClosed(handleOnConnectionClosed())
                .onError(handleOnError());

        // Connect to matchmaking service
        matchmakingModel.connect().exceptionally(ex -> {
            LOGGER.log(Level.SEVERE, "Failed to connect to matchmaking service", ex);
            Platform.runLater(() -> {
                Alert errorAlert = new Alert(Alert.AlertType.ERROR);
                errorAlert.setTitle("Connection Error");
                errorAlert.setHeaderText("Failed to connect to matchmaking service");
                errorAlert.setContentText("Please try again later.");
                errorAlert.show();
            });
            return null;
        });
    }


    private void startImageSpin() {
        RotateTransition rotate = new RotateTransition(Duration.seconds(10), loadingBall);
        rotate.setByAngle(360);
        rotate.setCycleCount(RotateTransition.INDEFINITE);
        rotate.play();
    }

    private void returnToMainMenu() {
        rootPane.getProperties().put("controller", this);
        Main.showModals(ResourcePath.EXIT_MODAL.getPath(), rootPane);
    }

    public void cleanup() {
        LOGGER.info("Performing matchmaking controller cleanup");
        if (matchmakingModel != null && matchmakingModel.isConnected()) {
            try {
                matchmakingModel.disconnect();
            } catch (Exception e) {
                LOGGER.log(Level.WARNING, "Error during WebSocket disconnect", e);
            }
            matchmakingModel.close();
            matchmakingModel = null;
        }
    }

    public Runnable handleOnConnected() {
        return () -> {
            matchmakingModel.enterQueue();
        };
    }

    public Runnable handleOnQueueSuccess() {
        return () -> {
            LOGGER.info("Successfully entered matchmaking queue");
        };
    }

    public Consumer<JSONObject> handleOnMatchFound() {
        return response -> {
            if (matchFound) return;
            matchFound = true;

            LOGGER.log(Level.INFO, "Matchmaking queue has been entered: " + response);

            String roomId = response.getString("roomId");
            String opponent = response.getString("opponent");

            Main.roomId = roomId;
            Main.opponent = opponent;
            LOGGER.info("Match found! Room ID: " + roomId + ", Opponent: " + opponent);

            // Clean up BEFORE changing scene
            cleanup();

            // Switch to game room on UI thread
            Platform.runLater(() -> {
                Main.ChangeScene(ResourcePath.GAME_ROOM.getPath());
            });
        };
    }

    public Consumer<String> handleOnQueueTimeout() {
        return message -> {
            LOGGER.log(Level.INFO, "Queue timeout: {0}", message);
            Platform.runLater(() -> {
//                Alert timeoutAlert = new Alert(Alert.AlertType.INFORMATION);
//                timeoutAlert.setTitle("Queue Timeout");
//                timeoutAlert.setHeaderText("Matchmaking Queue Timeout");
//                timeoutAlert.setContentText("No match was found: " + message);
//                timeoutAlert.show();

                Platform.runLater(() -> {
                    Main.ChangeScene(ResourcePath.MAIN_MENU.getPath());
                });
            });
        };
    }

    public Runnable handleOnConnectionClosed() {
        return () -> {
            LOGGER.info("Connection to matchmaking service closed");
        };
    }

    public Consumer<Throwable> handleOnError() {
        return error -> {
            LOGGER.log(Level.WARNING, "Matchmaking error", error);
        };
    }
}