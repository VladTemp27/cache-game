package org.amalzen.app.game_room;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;
import org.amalzen.app.ResourcePath;

import java.util.logging.Level;
import java.util.logging.Logger;

public class GameRoomApp extends Application {
    private static final Logger LOGGER = Logger.getLogger(GameRoomApp.class.getName());

    // Default game parameters
    private static String gameId = "test-game";
    private static int playerId = 0;
    private static String username = "player";
    private static boolean verbose = true;

    @Override
    public void start(Stage primaryStage) throws Exception {
        // Configure logging based on verbose flag
        configureLogging(verbose);

        // Process parameters from JavaFX
        Parameters params = getParameters();
        if (params.getRaw().size() > 0) gameId = params.getRaw().get(0);
        if (params.getRaw().size() > 1) playerId = Integer.parseInt(params.getRaw().get(1));
        if (params.getRaw().size() > 2) username = params.getRaw().get(2);

        LOGGER.info("Starting game with: gameId=" + gameId +
                ", playerId=" + playerId + ", username=" + username);

        // Load the game room FXML
        FXMLLoader loader = new FXMLLoader(getClass().getResource(ResourcePath.GAME_ROOM.getPath()));
        Parent root = loader.load();

        // Initialize the controller with parameters
        GameRoomController controller = loader.getController();
        controller.setGameParameters(gameId, playerId, username);

        // Create the scene
        Scene scene = new Scene(root);
        primaryStage.setTitle("CACHE Game - " + username);
        primaryStage.setScene(scene);
        primaryStage.setOnCloseRequest(e -> controller.shutdown());
        primaryStage.show();
    }

    private static void configureLogging(boolean verbose) {
        // Configure root logger
        Logger rootLogger = Logger.getLogger("");
        rootLogger.setLevel(verbose ? Level.FINE : Level.INFO);

        // Suppress excessive websocket logging
        Logger.getLogger("org.amalzen.app.game_room.GameRoomModel").setLevel(
                verbose ? Level.FINE : Level.WARNING);
    }

    public static void main(String[] args) {
        // Process command line args
        if (args.length > 0) gameId = args[0];
        if (args.length > 1) playerId = Integer.parseInt(args[1]);
        if (args.length > 2) username = args[2];
        verbose = (args.length > 3 && args[3].equalsIgnoreCase("verbose"));

        launch(args);
    }
}