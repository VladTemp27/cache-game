package org.amalzen.app.audio;

import javafx.scene.media.Media;
import javafx.scene.media.MediaPlayer;
import org.amalzen.app.Main;

import java.net.URISyntaxException;

public class AudioHandler {
    private static MediaPlayer musicPlayer, effectPlayer;
    private static String currentMusic, currentEffect;

    public static void playSound(String path) {
        try {
            if (path == null) {
                stopAudio(musicPlayer);
                currentMusic = null;
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

    private static void startAudio(String path, double volume, int cycleCount, boolean isMusic) throws URISyntaxException {
        Media media = new Media(Main.class.getResource(path).toURI().toString());
        MediaPlayer player = new MediaPlayer(media);
        player.setOnError(() -> {
            System.err.println("Error playing audio: " + player.getError().getMessage());
        });
        player.setCycleCount(cycleCount);
        player.setVolume(volume);
        player.play();

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
}
