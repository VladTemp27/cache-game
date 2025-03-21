package org.amalzen.app.modals;

import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.layout.AnchorPane;
import org.amalzen.app.Main;
import org.amalzen.app.ResourcePath;

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
            // Find the parent controller (MatchmakingController) to cancel matchmaking
            AnchorPane parentPane = (AnchorPane) rootExitModalPane.getParent();
            if (parentPane != null) {
                // Get the controller from the scene
                Object controller = parentPane.getProperties().get("controller");
                if (controller instanceof org.amalzen.app.match_making.MatchmakingController) {
                    // Call cleanup to properly close WebSocket connections
                    ((org.amalzen.app.match_making.MatchmakingController) controller).cleanup();
                }
            }

            // Then change scene to main menu
            Main.ChangeScene(ResourcePath.MAIN_MENU.getPath());
        });
    }

}
