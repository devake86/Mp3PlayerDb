package app.mp3playerdb;

import javafx.scene.media.Media;
import javafx.scene.media.MediaPlayer;
import javafx.util.Duration;

import java.io.File;

public class Mp3Player {
    private MediaPlayer mediaPlayer;
    private double volume = 0.5;

    public void loadMp3File(String songFilePath) {
        closeOldPlayer();

        File file = new File(songFilePath);

        Media media = new Media(file.toURI().toString());

        mediaPlayer = new MediaPlayer(media);

        mediaPlayer.setVolume(volume);
    }

    public void play() {
        if (mediaPlayer != null) {
            mediaPlayer.play();
        }
    }

    public void pause() {
        if (mediaPlayer != null) {
            mediaPlayer.pause();
        }
    }

    public void closeOldPlayer() {
        if (mediaPlayer != null) {
            mediaPlayer.stop();
            mediaPlayer.dispose();
            mediaPlayer = null;
        }
    }

    public void setVolume(double volume) {
        this.volume = volume;

        if (mediaPlayer != null) {
            mediaPlayer.setVolume(volume);
        }
    }

    public void seekDuration(double seconds) {
        if (mediaPlayer != null) {
            mediaPlayer.seek(Duration.seconds(seconds));
        }
    }

    public MediaPlayer getMediaPlayer() {
        return mediaPlayer;
    }
}
