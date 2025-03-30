package org.amalzen.app;

import javafx.application.Application;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.layout.AnchorPane;
import javafx.scene.media.Media;
import javafx.scene.media.MediaPlayer;
import javafx.stage.Stage;

import java.io.File;

public class Main extends Application {
    @FXML
    public static Scene scene;
    public static FXMLLoader fxmlLoader;
    public static Stage primaryStage;

    // fields for user
    public static String username;
    public static String sessionId;

    // fields for game room
    public static String roomId;
    public static String opponent;

    public static MediaPlayer mediaPlayer;
    private static String currentMusic;


    @Override
    public void start(Stage primaryStage) {
        Main.primaryStage = primaryStage;
        ChangeScene(ResourcePath.LOGIN.getPath());

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

    public static void playMusic(String path) {
        try {
            if (path == null) {
                if (mediaPlayer != null) {
                    mediaPlayer.stop();
                    mediaPlayer.dispose();
                    mediaPlayer = null;
                }
                currentMusic = null;
                return;
            }

            if ((path != null && path.equals(currentMusic))) {
                return;
            }

            currentMusic = path;

            if (mediaPlayer != null) {
                mediaPlayer.stop();
                mediaPlayer.dispose();
                mediaPlayer = null;
            }

            Media media = new Media(Main.class.getResource(path).toURI().toString());
            mediaPlayer = new MediaPlayer(media);
            mediaPlayer.setOnError(() -> {
                System.err.println("Error playing audio: " + mediaPlayer.getError().getMessage());
            });
            mediaPlayer.setCycleCount(MediaPlayer.INDEFINITE);
            mediaPlayer.setVolume(0.1);
            mediaPlayer.play();
        } catch (Exception e) {
            System.err.println("Failed to play music: " + e.getMessage());
        }
    }

    public static void main(String[] args) {
        launch(args);
    }
}