package org.amalzen.app.game_room;

import javafx.animation.RotateTransition;
import javafx.fxml.FXML;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.image.ImageView;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.StackPane;
import javafx.scene.transform.Rotate;
import javafx.util.Duration;

public class GameRoomController {
    @FXML private AnchorPane gameRoomPane;
    @FXML private Label roundNumber;
    @FXML private Label timePerTurn;
    @FXML private Label rivalScore;
    @FXML private Label homeScore;
    @FXML private Button cancelButton;
    @FXML private Button
            cardEasyButton1, cardEasyButton2, cardEasyButton3, cardEasyButton4, cardEasyButton5, cardEasyButton6,
            cardEasyButton7, cardEasyButton8, cardEasyButton9, cardEasyButton10, cardEasyButton11, cardEasyButton12,
            cardEasyButton13, cardEasyButton14, cardEasyButton15, cardEasyButton16;
    @FXML private StackPane
            card1, card2, card3, card4, card5, card6, card7, card8,
            card9, card10, card11, card12, card13, card14, card15, card16;
    @FXML private ImageView
            cardBack1, cardBack2, cardBack3, cardBack4, cardBack5, cardBack6,  cardBack7, cardBack8,
            cardBack9, cardBack10, cardBack11, cardBack12, cardBack13, cardBack14, cardBack15, cardBack16;
    @FXML private ImageView
            cardFront1, cardFront2, cardFront3, cardFront4, cardFront5, cardFront6, cardFront7, cardFront8,
            cardFront9, cardFront10, cardFront11, cardFront12, cardFront13, cardFront14, cardFront15, cardFront16;
    private boolean isFlipped = false;
    @FXML
    public void flipCard() {
        RotateTransition rotateOut = new RotateTransition(Duration.millis(300), card1);
        rotateOut.setAxis(Rotate.Y_AXIS);  // Set rotation along Y-axis
        rotateOut.setFromAngle(0);
        rotateOut.setToAngle(90);

        rotateOut.setOnFinished(event -> {
            // Toggle image visibility at 90-degree mark
            cardFront1.setOpacity(isFlipped ? 1 : 0);
            cardBack1.setOpacity(isFlipped ? 0 : 1);
            isFlipped = !isFlipped;

            // Rotate back from 90° to 180° to complete the flip
            RotateTransition rotateIn = new RotateTransition(Duration.millis(300), card1);
            rotateIn.setAxis(Rotate.Y_AXIS);
            rotateIn.setFromAngle(90);
            rotateIn.setToAngle(180);
            rotateIn.play();
        });

        rotateOut.play();
    }
}
