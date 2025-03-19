package org.amalzen.app.match_making;

import javafx.animation.RotateTransition;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.image.ImageView;
import javafx.util.Duration;
import org.amalzen.app.Main;
import org.amalzen.app.ResourcePath;

public class MatchmakingController {

    @FXML
    private Button cancelButton;
    @FXML
    private ImageView loadingBall;

    @FXML
    private void initialize() {
        startImageSpin();
        cancelButton.setOnMouseClicked(event -> returnToMainMenu());
    }

    private void startImageSpin() {
        RotateTransition rotate = new RotateTransition(Duration.seconds(10), loadingBall);
        rotate.setByAngle(360);
        rotate.setCycleCount(RotateTransition.INDEFINITE);
        rotate.play();
    }

    private void returnToMainMenu() {
        Main.ChangeScene(ResourcePath.MAIN_MENU.getPath());
    }

}
