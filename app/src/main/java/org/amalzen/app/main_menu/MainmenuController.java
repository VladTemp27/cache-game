package org.amalzen.app.main_menu;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.control.Button;
import javafx.scene.layout.AnchorPane;
import javafx.stage.Stage;
import org.amalzen.app.Main;
import org.amalzen.app.ResourcePath;
import org.amalzen.app.game_instructions.GameInstructionsController;

import java.io.IOException;

public class MainmenuController {
    public static GameInstructionsController gameInstructionsController;

    @FXML
    private AnchorPane mainMenuRootPane;

    @FXML
    private Button howToPlayButton;

    @FXML
    private Button playButton;

    public static Stage primaryStage;

    @FXML
    void showPage(ActionEvent event) {
        Button clickButton = (Button) event.getSource();
        String fxmlFile = null;
        if (clickButton == playButton) {
            // Navigate to matchmaking view and start queue
            Main.ChangeScene(ResourcePath.MATCHMAKING.getPath());
        } else if (clickButton == howToPlayButton) {
            Main.ChangeScene(ResourcePath.INSTRUCTION.getPath());
        }
    }

    // Showing the exit modal
    @FXML
    void showExitModal(ActionEvent event) {
        showMainMenuModals("/org/amalzen/app/view/exit-modal.fxml");
    }

    @FXML
    void showLogoutModal(ActionEvent event) {
        showMainMenuModals("/org/amalzen/app/view/logout-modal.fxml");
    }

    private void showMainMenuModals(String modalLocation) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource(modalLocation));
            Parent exitModalRoot = loader.load();

            mainMenuRootPane.getChildren().add(exitModalRoot);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }


    @FXML
    private void initialize() {

    }
}
