package org.amalzen.app.match_history;

import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.HBox;

public class MatchHistoryController {
    @FXML
    private AnchorPane matchHistoryPane;
    @FXML
    private HBox HBox1;
    @FXML
    private Button backButton;
    @FXML
    private Label resultLabel;
    @FXML
    private Label scoreLabel;
    @FXML
    private ImageView indicatorImage;

    public void updateMatchEntry(boolean isWin, String score) {
        if (isWin) {
            resultLabel.setText("Victory");
            indicatorImage.setImage(new Image(getClass().getResourceAsStream("../images/match-victory.png")));
        } else {
            resultLabel.setText("Defeat");
            indicatorImage.setImage(new Image(getClass().getResourceAsStream("../images/match-defeat.png")));
        }
        scoreLabel.setText(score);
    }

    public void handleBack() {

    }
}
