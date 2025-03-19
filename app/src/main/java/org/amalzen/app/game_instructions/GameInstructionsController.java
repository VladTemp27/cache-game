package org.amalzen.app.game_instructions;

import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.stage.Stage;
import org.amalzen.app.Main;
import org.amalzen.app.ResourcePath;

import java.io.IOException;

public class GameInstructionsController {

    @FXML
    private Button backButton;

    public void initialize() {
        backButton.setOnMouseClicked(event -> {
            Main.ChangeScene(ResourcePath.MAIN_MENU.getPath());
        });
    }
}
