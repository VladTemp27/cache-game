package org.amalzen.app.audio;

import javafx.scene.control.Alert;
import javafx.scene.media.Media;
import javafx.scene.media.MediaException;
import javafx.scene.media.MediaPlayer;
import org.amalzen.app.Main;

import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.Mixer;
import java.net.URISyntaxException;

public class AudioHandler {
    private static MediaPlayer musicPlayer, effectPlayer;
    private static String currentMusic, currentEffect;
    private static boolean audioOutputAlerted = false;

    public static void playSound(String path) {
        try {
            if (path == null) {
                stopAudio(musicPlayer);
                currentMusic = null;
                return;
            }

            if (!isAudioOutputAvailable()) {
                if (!audioOutputAlerted) {
                    showAlert("No audio output detected. Sound will not play.");
                    audioOutputAlerted = true;
                }
                return;
            }

            if (path.contains("/music/")) {
                if (path.equals(currentMusic)) {
                    return; // already playing
                }
                currentMusic = path;
                stopAudio(musicPlayer);
                startAudio(path, 0.1, MediaPlayer.INDEFINITE, true);
            } else if (path.contains("/effects/")) {
                if (path.equals(currentEffect)) {
                    stopAudio(effectPlayer);
                }
                currentEffect = path;
                startAudio(path, 0.8, 1, false);
            } else {
                System.err.println("Unknown audio type for path: " + path);
            }
        } catch (Exception e) {
            System.err.println("Failed to play music: " + e.getMessage());
        }
    }

    private static boolean isAudioOutputAvailable() {
        Mixer.Info[] mixers = AudioSystem.getMixerInfo();
        for (Mixer.Info mixerInfo : mixers) {
            Mixer mixer = AudioSystem.getMixer(mixerInfo);
            if (mixer.isLineSupported(javax.sound.sampled.Port.Info.SPEAKER) ||
                    mixer.isLineSupported(javax.sound.sampled.Port.Info.HEADPHONE)) {
                return true;
            }
        }
        return false;
    }

    private static void startAudio(String path, double volume, int cycleCount, boolean isMusic) throws URISyntaxException {
        Media media = new Media(Main.class.getResource(path).toURI().toString());
        MediaPlayer player = new MediaPlayer(media);

        player.setOnReady(() -> {
            System.out.println("Audio loaded successfully: " + path);
            player.setCycleCount(cycleCount);
            player.setVolume(volume);
            player.play();
        });

        player.setOnError(() -> {
            MediaException error = player.getError();
            System.err.println("Error playing audio: " + error.getMessage());
            handleAudioError(error);
        });

        if (isMusic) {
            musicPlayer = player;
        } else {
            effectPlayer = player;
        }
    }


    private static void stopAudio(MediaPlayer player) {
        if (player != null) {
            try {
                if (player.getStatus() != MediaPlayer.Status.DISPOSED && player.getStatus() != MediaPlayer.Status.UNKNOWN) {
                    player.stop();
                    player.dispose();
                }
            } catch (Exception e) {
                System.err.println("Error stopping audio: " + e.getMessage());
            }
        }
    }

    public static void stopMusic() {
        stopAudio(musicPlayer);
        currentMusic = null;
    }

    private static void handleAudioError(MediaException error) {
        String message = switch (error.getType()) {
            case MEDIA_UNAVAILABLE -> "No audio output detected.";
            case MEDIA_UNSUPPORTED -> "No audio device found.";
            case MEDIA_UNSPECIFIED -> "No audio drivers found.";
            default -> "An unknown audio error occurred.";
        };
        showAlert(message);
    }

    private static void showAlert(String message) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle("Audio Error");
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }
}
