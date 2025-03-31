package org.amalzen.app.leaderboards;

import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.VBox;
import org.amalzen.app.Main;
import org.amalzen.app.ResourcePath;
import java.io.IOException;

public class LeaderboardController {

    // Elements from leaderboard-view.fxml
    @FXML
    private Button backButton;
    @FXML
    private VBox leaderboardUserContainer;

    // Elements from leaderboard-component.fxml
    @FXML
    private Label rank;
    @FXML
    private Label username;
    @FXML
    private Label score;

    public void initialize() {
        if (backButton != null) {
            backButton.setOnMouseClicked(event -> {
                Main.ChangeScene(ResourcePath.MAIN_MENU.getPath());
            });
        }

        if (leaderboardUserContainer != null) {
            // Load and add leaderboard components
            addLeaderboardComponents();
        }
        colorPerRank();
    }

// TODO: Fetch the rank number from the server
private void colorPerRank() {
    for (Node node : leaderboardUserContainer.getChildren()) {
        if (node instanceof AnchorPane) {
            AnchorPane anchorPane = (AnchorPane) node;
            Label rankLabel = (Label) anchorPane.lookup("#rank");
            int rank = Integer.parseInt(rankLabel.getText());

            if (rank == 1) {
                anchorPane.setStyle("-fx-background-color: #725B9D;");
            } else if (rank == 2) {
                anchorPane.setStyle("-fx-background-color: #FF017B;");
            } else if (rank == 3) {
                anchorPane.setStyle("-fx-background-color: #2AAEE7;");
            } else {
                anchorPane.setStyle("-fx-background-color: #5B3A29;");
            }
        }
    }
}

    // TODO: Implement the logic to fetch leaderboard data from the server
    private void addLeaderboardComponents() {
        try {
            for (int i = 0; i < 10; i++) {
                FXMLLoader loader = new FXMLLoader(getClass().getResource("/org/amalzen/app/view/leaderboard-component.fxml"));
                AnchorPane component = loader.load();
                leaderboardUserContainer.getChildren().add(component);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}