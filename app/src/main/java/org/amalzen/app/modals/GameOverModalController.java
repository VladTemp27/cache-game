package org.amalzen.app.modals;

import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.effect.ColorAdjust;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.AnchorPane;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import org.amalzen.app.ResourcePath;
import org.amalzen.app.audio.AudioHandler;

public class GameOverModalController {

    public enum GameOutcome {
        WIN, LOSE, TIE
    }

    @FXML
    private AnchorPane rootGameOverModalPane;
    @FXML
    private Button backButton;
    @FXML
    private Button playAgainButton;
    @FXML
    private Label resultLabel;
    @FXML
    private Label subTitleLabel1;
    @FXML
    private Label subTitleLabel2;
    @FXML
    private ImageView playAgainImageView;

    private GameOutcome gameOutcome;

    public void setGameOutcome(GameOutcome outcome) {
        this.gameOutcome = outcome;
        updateUI();
    }

    private void updateUI() {
        switch (gameOutcome) {
            case WIN:
                resultLabel.setText("VICTORY!");
                resultLabel.setTextFill(Color.valueOf("#024d16"));  // Green
                subTitleLabel1.setText("From the Undercity to the top—");
                subTitleLabel2.setText("you reign supreme!");
                playAgainImageView.setImage(new Image(getClass().getResourceAsStream("/org/amalzen/app/images/victory-playagain.png")));
                break;

            case LOSE:
                resultLabel.setText("DEFEAT!");
                resultLabel.setTextFill(Color.valueOf("#860C0C"));  // Red
                subTitleLabel1.setText("You lost this battle, but the war");
                subTitleLabel2.setText("is never over.");
                playAgainImageView.setImage(new Image(getClass().getResourceAsStream("/org/amalzen/app/images/defeat-playagain.png")));
                break;

            case TIE:
                resultLabel.setText("TIE GAME!");
                resultLabel.setTextFill(Color.valueOf("#644C00"));  // Gold
                subTitleLabel1.setText("Equally matched in skill and wit—");
                subTitleLabel2.setText("another round to settle it?");
                playAgainImageView.setImage(new Image(getClass().getResourceAsStream("/org/amalzen/app/images/victory-playagain.png")));
                break;
        }
    }

    private void addHoverEffectImage(Button button) {
        ImageView imageView = (ImageView) button.getGraphic();
        ColorAdjust colorAdjust = new ColorAdjust();

        button.setOnMouseEntered(e -> {
            colorAdjust.setBrightness(-0.3);
            imageView.setEffect(colorAdjust);
        });

        button.setOnMouseExited(e -> {
            colorAdjust.setBrightness(0);
            imageView.setEffect(colorAdjust);
        });
    }

    @FXML
    public void handleBack() {
        // Find the parent GameRoomController to properly clean up resources
        AnchorPane parentPane = (AnchorPane) rootGameOverModalPane.getParent();
        if (parentPane != null) {
            Object controller = parentPane.getProperties().get("controller");

            if (controller instanceof org.amalzen.app.game_room.GameRoomController) {
                ((org.amalzen.app.game_room.GameRoomController) controller).shutdown();
            }
        }

        // Return to main menu
        org.amalzen.app.Main.ChangeScene(org.amalzen.app.ResourcePath.MAIN_MENU.getPath());
    }

    @FXML
    public void handlePlayAgain() {
        // Find the parent GameRoomController to properly clean up resources
        AnchorPane parentPane = (AnchorPane) rootGameOverModalPane.getParent();
        if (parentPane != null) {
            Object controller = parentPane.getProperties().get("controller");

            if (controller instanceof org.amalzen.app.game_room.GameRoomController) {
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

        resultLabel.setFont(gloockFont);
        subTitleLabel1.setFont(gloryFont);
        subTitleLabel2.setFont(gloryFont);

        addHoverEffectImage(backButton);
        backButton.setOnAction(event -> handleBack());
        playAgainButton.setOnAction(event -> handlePlayAgain());

        // Default to WIN if not set
        if (gameOutcome == null) {
            gameOutcome = GameOutcome.WIN;
        }
        updateUI();
    }
}