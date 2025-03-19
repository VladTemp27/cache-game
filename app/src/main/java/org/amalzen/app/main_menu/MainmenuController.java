package org.amalzen.app.main_menu;

import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.image.ImageView;
import javafx.stage.Stage;

import java.io.IOException;

public class MainmenuController {

    @FXML
    private ImageView gameHistoryButton;

    @FXML
    private ImageView howToPlayButton;

    @FXML
    private ImageView leaderboardsButton;

    @FXML
    private ImageView playButton;

    @FXML
    private ImageView userButton;

    // TODO: Insert fxml file paths of corresponding views
    public void initialize() {
//        userButton.setOnMouseClicked(event -> navigateToView(""));
        howToPlayButton.setOnMouseClicked(event -> navigateToView("/org/amalzen/app/view/game-instructions.fxml"));
        playButton.setOnMouseClicked(event -> navigateToView(""));
//        gameHistoryButton.setOnMouseClicked(event -> navigateToView(""));
//        leaderboardsButton.setOnMouseClicked(event -> navigateToView(""));
    }

    private void navigateToView(String file) {
        try {
            Stage stage = (Stage) playButton.getScene().getWindow();
            FXMLLoader fxmlLoader = new FXMLLoader(getClass().getResource(file));
            Scene scene = new Scene(fxmlLoader.load());
            stage.setScene(scene);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

}
