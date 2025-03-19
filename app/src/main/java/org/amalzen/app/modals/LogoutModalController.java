package org.amalzen.app.modals;

import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.layout.AnchorPane;
import org.amalzen.app.Main;
import org.amalzen.app.ResourcePath;

public class LogoutModalController {

    @FXML
    private Button cancelLogoutButton;

    @FXML
    private Button confirmLogoutButton;

    @FXML
    private AnchorPane rootLogoutModalPane;

    public void initialize() {
        cancelLogoutButton.setOnAction(event -> {
            rootLogoutModalPane.setVisible(false);
        });

        confirmLogoutButton.setOnAction(event -> {
            Main.ChangeScene(ResourcePath.LOGIN.getPath());
        });
    }
}
