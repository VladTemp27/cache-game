package org.amalzen.app.main_menu;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.layout.AnchorPane;
import org.amalzen.app.Main;
import org.amalzen.app.ResourcePath;

public class MainmenuController {
    @FXML
    private Button howToPlayButton;

    @FXML
    private Button playButton;

    @FXML
    private Button logoutButton;

    @FXML
    public AnchorPane rootPane;

    @FXML
    void showPage(ActionEvent event) {
        Button clickButton = (Button) event.getSource();
        if (clickButton == playButton) {
            Main.ChangeScene(ResourcePath.MATCHMAKING.getPath());
        } else if (clickButton == logoutButton) {
            Main.showModals(ResourcePath.LOGOUT_MODAL.getPath(), rootPane);
        } else if (clickButton == howToPlayButton) {
            Main.ChangeScene(ResourcePath.INSTRUCTION.getPath());
        }
    }

    @FXML
    private void initialize() {
        Main.playMusic(ResourcePath.MAIN_MENU_MUSIC.getPath());
    }
}
