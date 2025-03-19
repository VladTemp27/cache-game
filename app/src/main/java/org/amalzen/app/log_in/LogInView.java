package org.amalzen.app.log_in;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.image.Image;
import javafx.stage.Stage;
import java.io.IOException;

public class LogInView extends Application {

    @Override
    public void start(Stage primaryStage) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/org/amalzen/app/view/login-view.fxml"));
            Parent root = loader.load();
            if (root == null) {
                System.out.println("FXML file not loaded properly!");
                return;
            }
            Scene scene = new Scene(root, 1650, 944);
            primaryStage.getIcons().add(new Image(getClass().getResource("/org/amalzen/app/images/game-logo-transparent.png").toString()));
            primaryStage.setTitle("CACHE");
            primaryStage.setScene(scene);
            primaryStage.setResizable(false);
            primaryStage.show();
        } catch (IOException e) {
            e.printStackTrace();
            System.out.println("Error loading FXML: " + e.getMessage());
        }
    }
    public static void main(String[] args) {
        launch(args);
    }
}
