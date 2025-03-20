package org.amalzen.app;

import javafx.application.Application;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.layout.AnchorPane;
import javafx.stage.Stage;
import org.amalzen.app.util.SessionStorage;

public class Main extends Application {
    @FXML
    public static Scene scene;
    public static FXMLLoader fxmlLoader;
    public static Stage primaryStage;

    @Override
    public void start(Stage primaryStage) {
        Main.primaryStage = primaryStage;

        // Check if there's an existing session
        String sessionId = SessionStorage.get("sessionId");
        if (sessionId != null && !sessionId.isEmpty()) {
            // User already logged in, go directly to main menu
            ChangeScene(ResourcePath.MAIN_MENU.getPath());
        } else {
            // No existing session, go to login screen
            ChangeScene(ResourcePath.LOGIN.getPath());
        }

        primaryStage.setTitle("CACHE");
        primaryStage.setResizable(false);
        primaryStage.show();
    }

    public static void ChangeScene(String path) {
        fxmlLoader = new FXMLLoader(Main.class.getResource(path));
        try {
            scene = new Scene(fxmlLoader.load());
            primaryStage.setScene(scene);
        } catch (Exception e) {
            System.err.println("Scene Error: " + e.getMessage());
//            e.printStackTrace();
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