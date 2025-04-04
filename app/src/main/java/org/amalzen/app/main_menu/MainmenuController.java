package org.amalzen.app.main_menu;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.AnchorPane;
import org.amalzen.app.Main;
import org.amalzen.app.ResourcePath;
import org.amalzen.app.audio.AudioHandler;

public class MainmenuController {
    @FXML
    private Button howToPlayButton;

    @FXML
    private ImageView toggleMusicImageView;

    @FXML
    private Button playButton;

    @FXML
    private Button logoutButton;

    @FXML
    private Button leaderboardButton;

    @FXML
    public AnchorPane rootPane;

    private Image musicOnIcon;
    private Image musicOffIcon;

    @FXML
    void showPage(ActionEvent event) {
        Button clickButton = (Button) event.getSource();
        if (clickButton == playButton) {
            Main.ChangeScene(ResourcePath.MATCHMAKING.getPath());
        } else if (clickButton == leaderboardButton) {
            Main.showModals(ResourcePath.LEADERBOARD.getPath(), rootPane);
        } else if (clickButton == logoutButton) {
            Main.showModals(ResourcePath.LOGOUT_MODAL.getPath(), rootPane);
        } else if (clickButton == howToPlayButton) {
            Main.ChangeScene(ResourcePath.INSTRUCTION.getPath());
        }
    }

    @FXML
    private void initialize() {
        AudioHandler.playSound(ResourcePath.MAIN_MENU_MUSIC.getPath());

        musicOnIcon = new Image(getClass().getResourceAsStream(ResourcePath.SOUND_ON_ICON.getPath()));
        musicOffIcon = new Image(getClass().getResourceAsStream(ResourcePath.SOUND_OFF_ICON.getPath()));

        toggleMusicImageView.setOnMouseClicked(event -> {
            AudioHandler.setMusicMuted(!AudioHandler.isMusicMuted());
            updateMusicIconState();
        });
        updateMusicIconState();
    }


    private void updateMusicIconState() {
        toggleMusicImageView.setImage(AudioHandler.isMusicMuted() ? musicOffIcon : musicOnIcon);
    }

}
