package org.amalzen.app;

import io.github.cdimascio.dotenv.Dotenv;
import javafx.application.Application;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.layout.AnchorPane;
import javafx.stage.Stage;

public class Main extends Application {
    @FXML
    public static Scene scene;
    public static FXMLLoader fxmlLoader;
    public static Stage primaryStage;

    @Override
    public void start(Stage primaryStage) {
        Main.primaryStage = primaryStage;
        ChangeScene(ResourcePath.MAIN_MENU.getPath()); // TODO: Change back to login
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
        Dotenv dotenv = Dotenv.load();
        System.out.println(dotenv.get("API_URL"));
        launch(args);
    }
}