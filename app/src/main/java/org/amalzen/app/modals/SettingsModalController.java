package org.amalzen.app.modals;

import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.image.Image;
import javafx.scene.layout.AnchorPane;
import org.amalzen.app.Main;
import org.amalzen.app.ResourcePath;
import org.amalzen.app.audio.AudioHandler;
import javafx.scene.image.ImageView;

public class SettingsModalController {

    @FXML
    private Button quitButton;

    @FXML
    private AnchorPane rootSettingsModalPane;

    @FXML
    private ImageView effectsImageView;

    @FXML
    private ImageView musicImageView;

    private Image musicOnIcon;
    private Image musicOffIcon;
    private Image effectsOnIcon;
    private Image effectsOffIcon;

    @FXML
    private void initialize() {
        try {
            musicOnIcon = new Image(getClass().getResourceAsStream(ResourcePath.SOUND_ON_ICON.getPath()));
            musicOffIcon = new Image(getClass().getResourceAsStream(ResourcePath.SOUND_OFF_ICON.getPath()));
            effectsOnIcon = new Image(getClass().getResourceAsStream(ResourcePath.SOUND_ON_ICON.getPath()));
            effectsOffIcon = new Image(getClass().getResourceAsStream(ResourcePath.SOUND_OFF_ICON.getPath()));
        } catch (Exception e) {
            System.err.println("Error loading images: " + e.getMessage());
            e.printStackTrace();
        }

        musicImageView.setOnMouseClicked(event -> {
            AudioHandler.setMusicMuted(!AudioHandler.isMusicMuted());
            updateMusicIconState();
        });

        effectsImageView.setOnMouseClicked(event -> {
            AudioHandler.setEffectsMuted(!AudioHandler.isEffectsMuted());
            updateEffectsIconState();
        });

        updateMusicIconState();
        updateEffectsIconState();

        quitButton.setOnAction(event -> {
            Main.showModals(ResourcePath.EXIT_MODAL.getPath(), (AnchorPane) rootSettingsModalPane.getParent());
            rootSettingsModalPane.setVisible(false);
        });

        rootSettingsModalPane.setOnMouseClicked(event -> {
            if (event.getTarget() == rootSettingsModalPane) {
                rootSettingsModalPane.setVisible(false);
            }
        });
    }

    private void updateMusicIconState() {
        if (musicOnIcon != null && musicOffIcon != null) {
            musicImageView.setImage(AudioHandler.isMusicMuted() ? musicOffIcon : musicOnIcon);
        }
    }

    private void updateEffectsIconState() {
        if (effectsOnIcon != null && effectsOffIcon != null) {
            effectsImageView.setImage(AudioHandler.isEffectsMuted() ? effectsOffIcon : effectsOnIcon);
        }
    }

}
