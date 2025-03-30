package org.amalzen.app.leaderboards;

import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.control.Button;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.VBox;
import org.amalzen.app.Main;
import org.amalzen.app.ResourcePath;

public class LeaderboardController {

    @FXML
    private Button backButton;

    @FXML
    private VBox leaderboardUserContainer;

    public void initialize() {
        backButton.setOnMouseClicked(event -> {
            Main.ChangeScene(ResourcePath.MAIN_MENU.getPath());
        });
    }

}
