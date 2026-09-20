package app.mp3playerdb;

public class PlaylistEntry {
    private final int playlistPosition;
    private final Song song;

    public PlaylistEntry(int playlistPosition, Song song) {
        this.playlistPosition = playlistPosition;
        this.song = song;
    }

    public int getPlaylistPosition() {
        return playlistPosition;
    }

    public Song getSong() {
        return song;
    }

    public String getSongArtist() {
        return song.getSongArtist();
    }

    public String getSongTitle() {
        return song.getSongTitle();
    }
}
