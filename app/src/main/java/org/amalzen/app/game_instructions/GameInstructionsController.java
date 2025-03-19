package org.amalzen.app.game_instructions;

import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.stage.Stage;

import java.io.IOException;

public class GameInstructionsController {

    @FXML
    private Button backButton;

    public void initialize() {
        backButton.setOnMouseClicked(event -> {
            try {
                FXMLLoader loader = new FXMLLoader(getClass().getResource("/org/amalzen/app/view/mainmenu-view.fxml"));
                Parent gameInstructionsRoot = loader.load();
                Stage stage = (Stage) backButton.getScene().getWindow();
                Scene scene = new Scene(gameInstructionsRoot);
                stage.setScene(scene);
                stage.show();
            } catch (IOException e) {
                e.printStackTrace();
            }
        });
    }
}
