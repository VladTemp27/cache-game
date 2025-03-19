package org.amalzen.app.match_making;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;

import java.io.IOException;

public class MatchmakingView extends Application {
    @Override
    public void start(Stage primaryStage) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/org/amalzen/app/view/matchmaking-view.fxml"));
            Parent root = loader.load();
            if (root == null) {
                System.out.println("FXML file not loaded properly!");
                return;
            }
            Scene scene = new Scene(root, 1650, 944);
            primaryStage.setTitle("Please wait");
            primaryStage.setScene(scene);
            primaryStage.setResizable(false);
            primaryStage.show();
        } catch (IOException e) {
            e.printStackTrace();
            System.out.println("Error loading FXML: " + e.getMessage());
        }
    }
    public static void main(String[] args) {
        launch();
    }
}
