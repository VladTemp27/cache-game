package org.amalzen.app.game_room;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.VBox;
import org.amalzen.app.components.CardComponent;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class GameRoomController {
    private static final int ROWS = 2; // this can be changed if we opt to continue with different game difficulties
    private static final int COLUMNS = 8;

    @FXML
    private AnchorPane gameRoomPane;
    @FXML
    private Label roundNumber;
    @FXML
    private Label timePerTurn;
    @FXML
    private Label rivalScore;
    @FXML
    private Label homeScore;
    @FXML
    private Button cancelButton;
    @FXML
    private VBox cardsContainer;

    private List<CardComponent> cardComponents = new ArrayList<>();

    @FXML
    void showVictoryModal(ActionEvent event) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/org/amalzen/app/view/victory-modal.fxml"));
            Parent victoryModalRoot = loader.load();

            // Add the modal to the gameRoomPane
            gameRoomPane.getChildren().add(victoryModalRoot);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @FXML
    void showDefeatModal(ActionEvent event) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/org/amalzen/app/view/defeat-modal.fxml"));
            Parent defeatModalRoot = loader.load();

            // Add the modal to the gameRoomPane
            gameRoomPane.getChildren().add(defeatModalRoot);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @FXML
    public void initialize() {
        // TODO: This code will be used for MainController
        // gameVictory.setOnAction(this::showVictoryModal);
        // gameDefeat.setOnAction(this::showDefeatModal);

        try {
            GridPane cardGrid = new GridPane();
            cardGrid.setHgap(10);
            cardGrid.setVgap(10);

            for (int row = 0; row < ROWS; row++) {
                for (int col = 0; col < COLUMNS; col++) {
                    FXMLLoader loader = new FXMLLoader(getClass().getResource("/org/amalzen/app/view/card.fxml"));
                    Node cardNode = loader.load();
                    CardComponent cardComponent = loader.getController();

                    int cardId = row * 8 + col;
                    cardComponent.setCardId(cardId);
                    cardComponents.add(cardComponent);

                    cardGrid.add(cardNode, col, row);

                    // other logic to handle websocket communication will be added
                }
            }

            gameRoomPane.getChildren().add(cardGrid);
            AnchorPane.setTopAnchor(cardGrid, 200.0);
            AnchorPane.setLeftAnchor(cardGrid, 100.0);

        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}