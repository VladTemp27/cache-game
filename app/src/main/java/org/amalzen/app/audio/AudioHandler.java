package org.amalzen.app.audio;

import javafx.scene.media.Media;
import javafx.scene.media.MediaPlayer;
import org.amalzen.app.Main;

import java.net.URISyntaxException;

public class AudioHandler {
    public static MediaPlayer mediaPlayer;
    private static String currentMusic;

    public static void playMusic(String path) {
        try {
            if (path == null) {
                stopMusic();
                currentMusic = null;
                return;
            }

            if ((path != null && path.equals(currentMusic))) {
                return; // already playing
            }

            currentMusic = path;

            stopMusic();

            startMusic(path);
        } catch (Exception e) {
            System.err.println("Failed to play music: " + e.getMessage());
        }
    }

    private static void startMusic(String path) throws URISyntaxException {
        Media media = new Media(Main.class.getResource(path).toURI().toString());
        mediaPlayer = new MediaPlayer(media);
        mediaPlayer.setOnError(() -> {
            System.err.println("Error playing audio: " + mediaPlayer.getError().getMessage());
        });
        mediaPlayer.setCycleCount(MediaPlayer.INDEFINITE);
        mediaPlayer.setVolume(0.1);
        mediaPlayer.play();
    }

    private static void stopMusic() {
        if (mediaPlayer != null) {
            mediaPlayer.stop();
            mediaPlayer.dispose();
            mediaPlayer = null;
        }
    }
}
