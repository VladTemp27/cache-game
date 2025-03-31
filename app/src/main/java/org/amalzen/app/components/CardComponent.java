package org.amalzen.app.components;

import javafx.animation.ParallelTransition;
import javafx.animation.RotateTransition;
import javafx.animation.ScaleTransition;
import javafx.animation.TranslateTransition;
import javafx.fxml.FXML;
import javafx.geometry.Bounds;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.image.ImageView;
import javafx.scene.layout.StackPane;
import javafx.scene.text.Text;
import javafx.scene.transform.Rotate;
import javafx.util.Duration;
import org.amalzen.app.ResourcePath;
import org.amalzen.app.audio.AudioHandler;

public class CardComponent {
    @FXML
    private Button cardButton;
    @FXML
    private StackPane cardStackPane;
    @FXML
    private ImageView cardBack;
    @FXML
    private ImageView cardFront;
    @FXML
    private Text cardLabel;
    @FXML
    private StackPane cardFaces;

    private boolean isFlipped = false;
    private boolean isZoomed = false;
    private int cardId = 0;

    // Store original position and scale for reset
    private double originalX = 0;
    private double originalY = 0;
    private double originalScale = 1.0;

    @FXML
    private void initialize() {
        cardButton.setOnAction(event -> {
            flipCard();
            zoomToCenter();
            AudioHandler.playSound(ResourcePath.FLIP_CARD_SOUND.getPath());
        });
    }

    public void setCardId(int id) {
        this.cardId = id;
    }

    public int getCardId() {
        return cardId;
    }

    public void flipCard() {
        // Existing flip card implementation...
        RotateTransition rotateOut = new RotateTransition(Duration.millis(300), cardFaces);
        rotateOut.setAxis(Rotate.Y_AXIS);
        rotateOut.setFromAngle(0);
        rotateOut.setToAngle(90);

        rotateOut.setOnFinished(event -> {
            // Toggle image visibility at 90-degree mark
            cardFront.setOpacity(isFlipped ? 1 : 0);
            cardBack.setOpacity(isFlipped ? 0 : 1);
            cardLabel.setOpacity(isFlipped ? 0 : 1);
            isFlipped = !isFlipped;

            // Rotate back from 90° to 180° to complete the flip
            RotateTransition rotateIn = new RotateTransition(Duration.millis(300), cardFaces);
            rotateIn.setAxis(Rotate.Y_AXIS);
            rotateIn.setFromAngle(90);
            rotateIn.setToAngle(180);
            rotateIn.play();

            //Hotfix for card label
            RotateTransition rotateLabel = new RotateTransition(Duration.millis(300), cardLabel);
            rotateLabel.setAxis(Rotate.Y_AXIS);
            rotateLabel.setFromAngle(90);
            rotateLabel.setToAngle(0);
            rotateLabel.play();
        });
        rotateOut.play();
    }

    private void zoomToCenter() {
        if (cardStackPane.getScene() == null) return;

        Scene scene = cardStackPane.getScene();

        // Store parent node reference for proper Z-order handling
        if (!isZoomed) {
            // Save original position
            originalX = cardStackPane.getTranslateX();
            originalY = cardStackPane.getTranslateY();
            originalScale = cardStackPane.getScaleX();

            // Get scene dimensions and card position in scene coordinates
            double sceneWidth = scene.getWidth();
            double sceneHeight = scene.getHeight();
            Bounds cardBounds = cardStackPane.localToScene(cardStackPane.getBoundsInLocal());

            // Calculate absolute position for center (instead of relative translation)
            double targetX = (sceneWidth - cardBounds.getWidth() * 2.0) / 2.0 - cardBounds.getMinX() + cardStackPane.getTranslateX();
            double targetY = (sceneHeight - cardBounds.getHeight() * 2.0) / 2.0 - cardBounds.getMinY() + cardStackPane.getTranslateY();

            // Ensure the card will be on top of all other elements
            cardStackPane.toFront();
            // Set higher Z-order explicitly for better stacking control
            cardStackPane.setViewOrder(-1.0);  // Lower values appear in front

            // Create zoom-in animation with absolute positioning
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
            // Reset view order when zooming out
            cardStackPane.setViewOrder(0.0);

            // Create zoom-out animation
            TranslateTransition translate = new TranslateTransition(Duration.millis(300), cardStackPane);
            translate.setToX(originalX);
            translate.setToY(originalY);

            ScaleTransition scale = new ScaleTransition(Duration.millis(300), cardStackPane);
            scale.setToX(originalScale);
            scale.setToY(originalScale);

            ParallelTransition zoomOut = new ParallelTransition(translate, scale);
            zoomOut.play();

            isZoomed = false;
        }
    }
}