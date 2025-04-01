package org.amalzen.app;

import javafx.application.Application;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.layout.AnchorPane;
import javafx.stage.Stage;
import javafx.scene.image.Image;

public class Main extends Application {
    @FXML
    public static Scene scene;
    public static FXMLLoader fxmlLoader;
    public static Stage primaryStage;

    // NOTE THESE WILL BE CHANGED TO A HASHMAP
    // fields for user
    public static String username;
    public static String sessionId;

    // fields for game room
    public static String roomId;
    public static String opponent;


    @Override
    public void start(Stage primaryStage) {
        Main.primaryStage = primaryStage;
        ChangeScene(ResourcePath.LOGIN.getPath());

        primaryStage.setTitle("CACHE");
        primaryStage.setResizable(false);

        // Set the application icon
        primaryStage.getIcons().add(new Image(Main.class.getResourceAsStream(ResourcePath.GAME_ICON.getPath())));
        primaryStage.show();
    }

    public static void ChangeScene(String path) {
        fxmlLoader = new FXMLLoader(Main.class.getResource(path));
        try {
            scene = new Scene(fxmlLoader.load());
            primaryStage.setScene(scene);
        } catch (Exception e) {
            System.err.println("Scene Error: " + e.getMessage());
        }
    }

    public static void showModals(String path, AnchorPane rootPane) {
        try {
            FXMLLoader loader = new FXMLLoader(Main.class.getResource(path));
            Parent exitModalRoot = loader.load();
            rootPane.getChildren().add(exitModalRoot);
        } catch (Exception e) {
            System.err.println("Modal Error: " + e.getMessage());
        }
    }

    public static void main(String[] args) {
        launch(args);
    }
}