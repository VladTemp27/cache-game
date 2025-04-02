package org.amalzen.app.components;

import javafx.animation.ParallelTransition;
import javafx.animation.RotateTransition;
import javafx.animation.ScaleTransition;
import javafx.animation.TranslateTransition;
import javafx.fxml.FXML;
import javafx.geometry.Bounds;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.image.ImageView;
import javafx.scene.layout.Pane;
import javafx.scene.layout.StackPane;
import javafx.scene.text.Font;
import javafx.scene.transform.Rotate;
import javafx.util.Duration;

import java.util.ArrayList;
import java.util.List;

public class CardComponent {
    private static List<CardComponent> flippedCards = new ArrayList<>();
    @FXML
    public Button cardButton;
    @FXML
    private StackPane cardStackPane;
    @FXML
    private ImageView cardBack;
    @FXML
    private ImageView cardFront;
    @FXML
    private Label cardLabel;
    @FXML
    private StackPane cardFaces;

    private boolean isFlipped = false;
    private boolean isFlipping = false;
    private boolean isZoomed = false;
    private int cardId = 0;

    private static int flippedCardCount = 0;
    private static final int MAX_FLIPPED_CARDS = 2;

    // Store original position and scale for reset
    private double originalX = 0;
    private double originalY = 0;
    private double originalScale = 1.0;

    private Pane overlay;

    @FXML
    private void initialize() {
        cardLabel.setVisible(false);
        cardStackPane.setOnMouseClicked(event -> {
            if (flippedCardCount < MAX_FLIPPED_CARDS || isFlipped) {
                flipCard();
//                zoomToCenter();
            }
        });
    }

    public void setCardId(int id) {
        this.cardId = id;
    }

    public int getCardId() {
        return cardId;
    }

    public void flipCard() {
        cardLabel.setVisible(true);

        if (isFlipping) return;
        isFlipping = true;
        cardButton.setDisable(true); // Disable the button

        RotateTransition rotateOut = new RotateTransition(Duration.millis(300), cardFaces);
        rotateOut.setAxis(Rotate.Y_AXIS);
        rotateOut.setFromAngle(0);
        rotateOut.setToAngle(90);

        RotateTransition rotateLabelOut = new RotateTransition(Duration.millis(300), cardLabel);
        rotateLabelOut.setAxis(Rotate.Y_AXIS);
        rotateLabelOut.setFromAngle(0);
        rotateLabelOut.setToAngle(90);

        ParallelTransition parallelOut = new ParallelTransition(rotateOut, rotateLabelOut);
        parallelOut.setOnFinished(event -> {
            isFlipped = !isFlipped;

            cardFront.setVisible(!isFlipped);
            cardBack.setVisible(isFlipped);
            cardLabel.setVisible(isFlipped);

            cardFront.setOpacity(!isFlipped ? 1 : 0);
            cardBack.setOpacity(isFlipped ? 1 : 0);
            cardLabel.setOpacity(isFlipped ? 1 : 0);

            if (isFlipped) {
                cardBack.toFront();
                flippedCardCount++;
            } else {
                cardFront.toFront();
                flippedCardCount--;
            }

            RotateTransition rotateIn = new RotateTransition(Duration.millis(300), cardFaces);
            rotateIn.setAxis(Rotate.Y_AXIS);
            rotateIn.setFromAngle(90);
            rotateIn.setToAngle(0);

            RotateTransition rotateLabelIn = new RotateTransition(Duration.millis(300), cardLabel);
            rotateLabelIn.setAxis(Rotate.Y_AXIS);
            rotateLabelIn.setFromAngle(90);
            rotateLabelIn.setToAngle(0);

            ParallelTransition parallelIn = new ParallelTransition(rotateIn, rotateLabelIn);
            parallelIn.setOnFinished(e -> {
                isFlipping = false;
                cardButton.setDisable(false); // Re-enable the button
            });
            parallelIn.play();
        });

        parallelOut.play();
    }

    private void zoomToCenter() {
        if (cardStackPane.getScene() == null) return;

        Scene scene = cardStackPane.getScene();

        if (!isZoomed) {
            originalX = cardStackPane.getTranslateX();
            originalY = cardStackPane.getTranslateY();
            originalScale = cardStackPane.getScaleX();

            double sceneWidth = scene.getWidth();
            double sceneHeight = scene.getHeight();
            Bounds cardBounds = cardStackPane.localToScene(cardStackPane.getBoundsInLocal());

            double targetX;
            double targetY = (sceneHeight - cardBounds.getHeight() * 2.0) / 2.0 - cardBounds.getMinY() + cardStackPane.getTranslateY();

            if (flippedCards.isEmpty()) {
                targetX = (sceneWidth / 4.0) - cardBounds.getMinX() + cardStackPane.getTranslateX();
                flippedCards.add(this);
            } else {
                targetX = (3 * sceneWidth / 4.0) - cardBounds.getMinX() + cardStackPane.getTranslateX();
                flippedCards.add(this);
            }

            cardStackPane.toFront();
            cardStackPane.setViewOrder(-1.0);

            TranslateTransition translate = new TranslateTransition(Duration.millis(300), cardStackPane);
            translate.setToX(targetX);
            translate.setToY(targetY);

            ScaleTransition scale = new ScaleTransition(Duration.millis(300), cardStackPane);
            scale.setToX(2.0);
            scale.setToY(2.0);

            ParallelTransition zoomIn = new ParallelTransition(translate, scale);
            zoomIn.play();

            isZoomed = true;

        } else {
            cardStackPane.setViewOrder(0.0);

            TranslateTransition translate = new TranslateTransition(Duration.millis(300), cardStackPane);
            translate.setToX(originalX);
            translate.setToY(originalY);

            ScaleTransition scale = new ScaleTransition(Duration.millis(300), cardStackPane);
            scale.setToX(originalScale);
            scale.setToY(originalScale);

            ParallelTransition zoomOut = new ParallelTransition(translate, scale);
            zoomOut.play();

            isZoomed = false;
            flippedCards.remove(this);
        }
    }

    public void setCardLabel(String value) {
        if (cardLabel != null) {
            cardLabel.setText(value);

            // Define font size ranges
            final double MAX_FONT_SIZE = 20.0;
            final double MIN_FONT_SIZE = 15.0;

            // Get text length
            int length = value.length();
            double fontSize;
            double wrappingWidth;

            // Calculate font size with smoot   her transitions
            if (length <= 8) {
                fontSize = MAX_FONT_SIZE;
                wrappingWidth = 100.0;
            } else if (length <= 15) {
                fontSize = 18.0;
                wrappingWidth = 100.0;
            } else if (length <= 25) {
                fontSize = 16.0;
                wrappingWidth = 105.0;
            } else if (length <= 40) {
                fontSize = 15.0;
                wrappingWidth = 110.0;
            } else {
                fontSize = MIN_FONT_SIZE;
                wrappingWidth = 115.0;
            }

            // Enable text wrapping
            cardLabel.setWrapText(true);
            cardLabel.setMaxWidth(wrappingWidth);
            cardLabel.setPrefWidth(wrappingWidth);
            cardLabel.setTextAlignment(javafx.scene.text.TextAlignment.CENTER);

            // Apply the new font size
            Font currentFont = cardLabel.getFont();
            Font newFont = Font.font(currentFont.getFamily(), fontSize);
            cardLabel.setFont(newFont);
        }
    }

    public boolean isFlipped() {
        return isFlipped;
    }
}