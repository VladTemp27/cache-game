package org.amalzen.app.modals;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.layout.AnchorPane;
import org.amalzen.app.Main;
import org.amalzen.app.ResourcePath;
import org.amalzen.app.util.SessionStorage;

public class LogoutModalController {

    @FXML
    private Button cancelLogoutButton;

    @FXML
    private Button confirmLogoutButton;

    @FXML
    private AnchorPane rootLogoutModalPane;

    private static void handle(ActionEvent event) {
        SessionStorage.remove("sessionId");
        Main.ChangeScene(ResourcePath.LOGIN.getPath());
    }

    public void initialize() {
        cancelLogoutButton.setOnAction(event -> rootLogoutModalPane.setVisible(false));

        confirmLogoutButton.setOnAction(LogoutModalController::handle);
    }
}
