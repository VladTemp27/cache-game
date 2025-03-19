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
        // TODO: This code will be updated when MainController is ready
        // Sample: MainController.changeScreen(UIPathResolver.main_menu_path);
    }

    @FXML
    public void handlePlayAgain() {
        // TODO: This code will be updated when MainController is ready
        // Sample: MainController.changeScreen(UIPathResolver.main_menu_path);
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