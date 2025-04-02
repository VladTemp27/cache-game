package org.amalzen.app.modals;

import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.effect.ColorAdjust;
import javafx.scene.image.ImageView;
import javafx.scene.layout.AnchorPane;
import javafx.scene.text.Font;

public class VictoryModalController {
    @FXML
    private AnchorPane rootVictoryModalPane;
    @FXML
    private Button backButton;
    @FXML
    private Button playAgainButton;
    @FXML
    private Label victoryLabel;
    @FXML
    private Label subTitleLabel1;
    @FXML
    private Label subTitleLabel2;

    private void addHoverEffectImage(Button button) {
        ImageView imageView = (ImageView) button.getGraphic();
        ColorAdjust colorAdjust = new ColorAdjust();

        button.setOnMouseEntered(e -> {
            colorAdjust.setBrightness(-0.3); // Decrease brightness to make it darker
            imageView.setEffect(colorAdjust);
        });

        button.setOnMouseExited(e -> {
            colorAdjust.setBrightness(0); // Reset brightness
            imageView.setEffect(colorAdjust);
        });
    }

    @FXML
    public void handleBack() {
        // Find the parent GameRoomController to properly clean up resources
        AnchorPane parentPane = (AnchorPane) rootVictoryModalPane.getParent();
        if (parentPane != null) {
            Object controller = parentPane.getProperties().get("controller");

            if (controller instanceof org.amalzen.app.game_room.GameRoomController) {
                // Call shutdown to properly close WebSocket connections
                ((org.amalzen.app.game_room.GameRoomController) controller).shutdown();
            }
        }

        // Return to main menu
        org.amalzen.app.Main.ChangeScene(org.amalzen.app.ResourcePath.MAIN_MENU.getPath());
    }

    @FXML
    public void handlePlayAgain() {
        // Find the parent GameRoomController to properly clean up resources
        AnchorPane parentPane = (AnchorPane) rootVictoryModalPane.getParent();
        if (parentPane != null) {
            Object controller = parentPane.getProperties().get("controller");

            if (controller instanceof org.amalzen.app.game_room.GameRoomController) {
                // Call shutdown to properly close WebSocket connections
                ((org.amalzen.app.game_room.GameRoomController) controller).shutdown();
            }
        }

        // Go back to matchmaking screen
        org.amalzen.app.Main.ChangeScene(org.amalzen.app.ResourcePath.MATCHMAKING.getPath());
    }

    @FXML
    public void initialize() {
        Font gloockFont = Font.loadFont(getClass().getResourceAsStream("/org/amalzen/app/fonts/Gloock-Regular.ttf"), 80);
        Font gloryFont = Font.loadFont(getClass().getResourceAsStream("/org/amalzen/app/fonts/Glory-Regular.ttf"), 40);

        victoryLabel.setFont(gloockFont);
        subTitleLabel1.setFont(gloryFont);
        subTitleLabel2.setFont(gloryFont);

        addHoverEffectImage(backButton);
        backButton.setOnAction(event -> handleBack());
        playAgainButton.setOnAction(event -> handlePlayAgain());
    }
}