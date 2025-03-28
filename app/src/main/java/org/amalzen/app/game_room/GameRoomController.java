package org.amalzen.app.game_room;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.HBox;
import org.amalzen.app.Main;
import org.amalzen.app.ResourcePath;
import org.amalzen.app.components.CardComponent;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class GameRoomController {
    private static final int ROWS = 2; // FIXME: This should be dynamic when game difficulty is implemented
    private static final int COLUMNS = 8;
    public List<CardComponent> cardComponents = new ArrayList<>();
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
    private HBox HBox1;
    @FXML
    private HBox HBox2;

    @FXML
    void showVictoryModal(ActionEvent event) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/org/amalzen/app/view/victory-modal.fxml"));
            Parent victoryModalRoot = loader.load();

            // Add the modal to the gameRoomPane
            gameRoomPane.getChildren().add(victoryModalRoot);
        } catch (IOException e) {
            System.err.println("Error: " + e);
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
            System.err.println("Error: " + e);
        }
    }

    @FXML
    public void initialize() {
        // TODO: This code will be used for MainController
        // gameVictory.setOnAction(this::showVictoryModal);
        // gameDefeat.setOnAction(this::showDefeatModal);
        cancelButton.setOnMouseClicked(event -> {
            Main.showModals(ResourcePath.EXIT_MODAL.getPath(), gameRoomPane);
        });

        try {
            for (int row = 0; row < ROWS; row++) {
                HBox currentHBox = (row == 0) ? HBox1 : HBox2;

                for (int col = 0; col < COLUMNS; col++) {
                    FXMLLoader loader = new FXMLLoader(getClass().getResource(ResourcePath.CARD.getPath()));
                    Node cardNode = loader.load();
                    CardComponent cardComponent = loader.getController();

                    int cardId = row * 8 + col;
                    cardComponent.setCardId(cardId);
                    cardComponents.add(cardComponent);

                    currentHBox.getChildren().add(cardNode);

                    // other logic to handle websocket communication will be added
                }
            }

        } catch (IOException e) {
            System.err.println("Error: " + e);
        }
    }
}