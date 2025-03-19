package org.amalzen.app.match_making;

import javafx.animation.RotateTransition;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.image.ImageView;
import javafx.stage.Stage;
import javafx.util.Duration;
import java.io.IOException;

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
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/org/amalzen/app/view/mainmenu-view.fxml"));
            Parent mainMenuRoot = loader.load();

            Stage stage = (Stage) cancelButton.getScene().getWindow();
            Scene scene = new Scene(mainMenuRoot);

            stage.setScene(scene);
            stage.show();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
