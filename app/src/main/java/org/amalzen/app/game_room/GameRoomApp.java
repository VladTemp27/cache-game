package org.amalzen.app.game_room;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;
import org.amalzen.app.ResourcePath;

import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;

public class GameRoomApp extends Application {
    private static final Logger LOGGER = Logger.getLogger(GameRoomApp.class.getName());

    @Override
    public void start(Stage primaryStage) {
        try {
            // Correct path to the FXML file
            FXMLLoader loader = new FXMLLoader(getClass().getResource(ResourcePath.GAME_ROOM.getPath()));
            Parent root = loader.load();
//            GameRoomController controller = loader.getController();

            Scene scene = new Scene(root);
            primaryStage.setTitle("Game Room");
            primaryStage.setScene(scene);
            primaryStage.show();

            // Log that the scene was loaded successfully
            LOGGER.info("Game Room scene loaded successfully");

        } catch (IOException e) {
            LOGGER.log(Level.SEVERE, "Failed to load FXML file", e);
            e.printStackTrace();
        }
    }

    public static void main(String[] args) {
        launch(args);
    }
}