package org.amalzen.app.match_making;

import javafx.animation.PauseTransition;
import javafx.animation.RotateTransition;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.image.ImageView;
import javafx.scene.layout.AnchorPane;
import javafx.util.Duration;
import org.amalzen.app.Main;
import org.amalzen.app.ResourcePath;

public class MatchmakingController {

    @FXML
    private Button cancelButton;
    @FXML
    private ImageView loadingBall;
    @FXML
    public AnchorPane rootPane;
    final PauseTransition pause = new PauseTransition(Duration.seconds(2));


    @FXML
    private void initialize() {
        startImageSpin();
        cancelButton.setOnMouseClicked(event -> returnToMainMenu());

        // Move to game room after 2 seconds
        pause.setOnFinished(event -> Main.ChangeScene(ResourcePath.GAME_ROOM.getPath()));
        pause.play();

    }

    private void startImageSpin() {
        RotateTransition rotate = new RotateTransition(Duration.seconds(10), loadingBall);
        rotate.setByAngle(360);
        rotate.setCycleCount(RotateTransition.INDEFINITE);
        rotate.play();
    }

    private void returnToMainMenu() {
        pause.stop();
        Main.showModals(ResourcePath.EXIT_MODAL.getPath(), rootPane);

    }

}
