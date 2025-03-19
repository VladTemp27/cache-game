package org.amalzen.app.modals;

import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.layout.AnchorPane;

public class ExitModalController {

    @FXML
    private Button cancelExitButton;

    @FXML
    private Button confirmExitButton;

    @FXML
    private AnchorPane rootExitModalPane;

    public void initialize() {
        cancelExitButton.setOnAction(event -> {
            rootExitModalPane.setVisible(false);
        });

        confirmExitButton.setOnAction(event -> {
            // exit game
        });
    }

}
