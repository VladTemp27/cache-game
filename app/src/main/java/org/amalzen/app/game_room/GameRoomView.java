package org.amalzen.app.game_room;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.image.Image;
import javafx.stage.Stage;

import java.io.IOException;

public class GameRoomView extends Application {
    @Override
    public void start(Stage stage) throws IOException {
        FXMLLoader fxmlLoader = new FXMLLoader(GameRoomView.class.getResource("/org/amalzen/app/view/game-room.fxml"));
        Scene scene = new Scene(fxmlLoader.load(), 1650, 944);
        stage.setTitle("Game Room");

        var imageStream = getClass().getResourceAsStream("/org/amalzen/app/images/game-logo-transparent.png");
        if (imageStream != null) {
            stage.getIcons().add(new Image(imageStream));
        } else {
            System.out.println("Image resource not found!");
        }

        stage.setScene(scene);
        stage.show();
    }

    public static void main(String[] args) {
        launch();
    }
}