package org.amalzen.app.game_room;

import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
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
    public void initialize() {
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
            AnchorPane.setTopAnchor(cardGrid, 144.0);
            AnchorPane.setLeftAnchor(cardGrid, 46.0);

        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}