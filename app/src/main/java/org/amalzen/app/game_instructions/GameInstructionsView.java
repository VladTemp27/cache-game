package org.amalzen.app.game_instructions;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.stage.Stage;

import java.io.IOException;

public class GameInstructionsView extends Application {
    @Override
    public void start(Stage stage) throws IOException {
        FXMLLoader fxmlLoader = new FXMLLoader(GameInstructionsView.class.getResource("/org/amalzen/app/view/game_instructions.fxml"));
        Scene scene = new Scene(fxmlLoader.load(), 1650, 944);
        stage.setTitle("Hello!");
        stage.setScene(scene);
        stage.show();
    }

    public static void main(String[] args) {
        launch();
    }
}
