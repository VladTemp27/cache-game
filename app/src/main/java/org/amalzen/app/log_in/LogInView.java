package org.amalzen.app.log_in;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.image.Image;
import javafx.stage.Stage;
import java.io.IOException;
import java.util.Objects;

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
            primaryStage.setTitle("CACHE");

            var imageStream = getClass().getResourceAsStream("/org/amalzen/app/images/game-logo-transparent.png");
            if (imageStream != null) {
                primaryStage.getIcons().add(new Image(imageStream));
            } else {
                System.out.println("Image resource not found!");
            }

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
