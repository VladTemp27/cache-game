package org.amalzen.app.game_instructions;

import javafx.fxml.FXML;
import javafx.scene.control.Button;
import org.amalzen.app.Main;
import org.amalzen.app.ResourcePath;

public class GameInstructionsController {

    @FXML
    private Button backButton;

    public void initialize() {
        backButton.setOnMouseClicked(event -> {
            Main.ChangeScene(ResourcePath.MAIN_MENU.getPath());
        });
    }
}
