package org.amalzen.app.leaderboards;

import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.VBox;
import org.amalzen.app.Main;
import org.amalzen.app.ResourcePath;

import java.io.IOException;
import java.util.List;

public class LeaderboardController {
    private final LeaderboardModel leaderboardModel;

    @FXML
    private Button backButton;
    @FXML
    private VBox leaderboardUserContainer;

    public LeaderboardController() {
        this.leaderboardModel = new LeaderboardModel();
    }

    public void initialize() {
        if (backButton != null) {
            backButton.setOnMouseClicked(event -> Main.ChangeScene(ResourcePath.MAIN_MENU.getPath()));
        }

        if (leaderboardUserContainer != null) {
            refreshLeaderboard();
        }
    }

    private void refreshLeaderboard() {
        List<LeaderboardModel.LeaderboardEntry> entries = leaderboardModel.fetchLeaderboard();
        leaderboardUserContainer.getChildren().clear();

        try {
            int rank = 1;
            for (LeaderboardModel.LeaderboardEntry entry : entries) {
                FXMLLoader loader = new FXMLLoader(getClass().getResource(ResourcePath.LEADERBOARD_COMPONENT.getPath()));
                AnchorPane component = loader.load();

                Label rankLabel = (Label) component.lookup("#rank");
                Label usernameLabel = (Label) component.lookup("#username");
                Label scoreLabel = (Label) component.lookup("#score");

                rankLabel.setText(String.valueOf(rank));
                usernameLabel.setText(entry.username());
                scoreLabel.setText(String.valueOf(entry.score()));

                applyRankStyle(component, rank);
                leaderboardUserContainer.getChildren().add(component);
                rank++;
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void applyRankStyle(AnchorPane component, int rank) {
        String backgroundColor = switch (rank) {
            case 1 -> "#725B9D";
            case 2 -> "#FF017B";
            case 3 -> "#2AAEE7";
            default -> "#5B3A29";
        };
        component.setStyle("-fx-background-color: " + backgroundColor + ";");
    }
}