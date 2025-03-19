package org.amalzen.app.components;

import javafx.animation.RotateTransition;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.image.ImageView;
import javafx.scene.layout.StackPane;
import javafx.scene.transform.Rotate;
import javafx.util.Duration;

public class CardComponent {
    @FXML private Button cardButton;
    @FXML private StackPane cardStackPane;
    @FXML private ImageView cardBack;
    @FXML private ImageView cardFront;

    // TODO: Add attributes for card data


    private boolean isFlipped = false;
    private int cardId = 0;

    @FXML
    private void initialize() {
        cardButton.setOnAction(event -> flipCard());
    }

    public void setCardId(int id) {
        this.cardId = id;
    }

    public int getCardId() {
        return cardId;
    }

    public void flipCard() {
        RotateTransition rotateOut = new RotateTransition(Duration.millis(300), cardStackPane);
        rotateOut.setAxis(Rotate.Y_AXIS);
        rotateOut.setFromAngle(0);
        rotateOut.setToAngle(90);

        rotateOut.setOnFinished(event -> {
            // Toggle image visibility at 90-degree mark
            cardFront.setOpacity(isFlipped ? 1 : 0);
            cardBack.setOpacity(isFlipped ? 0 : 1);
            isFlipped = !isFlipped;

            // Rotate back from 90° to 180° to complete the flip
            RotateTransition rotateIn = new RotateTransition(Duration.millis(300), cardStackPane);
            rotateIn.setAxis(Rotate.Y_AXIS);
            rotateIn.setFromAngle(90);
            rotateIn.setToAngle(180);
            rotateIn.play();
        });

        rotateOut.play();
    }
}