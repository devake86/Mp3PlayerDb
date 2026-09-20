package app.mp3playerdb;

public class Song {
    private int songId;
    private String songArtist;
    private String songTitle;
    private String songAlbum;
    private int songYear;
    private String songGenre;
    private String songFilePath;

    public Song(String songArtist, String songTitle, String songAlbum, int songYear, String songGenre, String songFilePath) {
        this.songArtist = songArtist;
        this.songTitle = songTitle;
        this.songAlbum = songAlbum;
        this.songYear = songYear;
        this.songGenre = songGenre;
        this.songFilePath = songFilePath;
    }

    public Song(int songId, String songArtist, String songTitle, String songAlbum, int songYear, String songGenre, String songFilePath) {
        this(songArtist, songTitle, songAlbum, songYear, songGenre, songFilePath);
        this.songId = songId;
    }

    public int getSongId() {
        return songId;
    }

    public String getSongArtist() {
        return songArtist;
    }

    public String getSongTitle() {
        return songTitle;
    }

    public String getSongAlbum() {
        return songAlbum;
    }

    public int getSongYear() {
        return songYear;
    }

    public String getSongGenre() {
        return songGenre;
    }

    public String getSongFilePath() {
        return songFilePath;
    }
}
