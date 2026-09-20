package app.mp3playerdb;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class SongRepository {

    /*
        DATABASE CONFIGURATION
     */

    private static final String DB_FOLDER = "db";
    private static final String DB_URL = "jdbc:sqlite:" + DB_FOLDER + "/mp3playerdb.db";

    /*
        DATABASE INITIALIZATION
     */

    public SongRepository() {
        try {
            Files.createDirectories(Path.of(DB_FOLDER));
            createTable();
        } catch (IOException exc) {
            throw new RuntimeException("Could not create database folder", exc);
        }
    }

    private Connection connect() throws SQLException {
        Connection connection = DriverManager.getConnection(DB_URL);

        try (Statement statement = connection.createStatement()) {
            statement.execute("PRAGMA foreign_keys = ON;");
            return connection;
        } catch (SQLException exc) {
            connection.close();
            throw exc;
        }
    }

    private void createTable() {
        String sql = """
                CREATE TABLE IF NOT EXISTS songs (
                    song_id INTEGER PRIMARY KEY AUTOINCREMENT,
                    song_artist TEXT NOT NULL,
                    song_title TEXT NOT NULL,
                    song_album TEXT NOT NULL,
                    song_year INTEGER NOT NULL,
                    song_genre TEXT NOT NULL,
                    song_file_path TEXT NOT NULL UNIQUE
                );
                """;

        try (
                Connection connection = connect();
                Statement statement = connection.createStatement();
                ) {
            statement.execute(sql);
        } catch (SQLException exc) {
            throw new RuntimeException("Could not create table 'songs'.", exc);
        }
    }

    /*
        SONG DATABASE OPERATIONS
     */

    public void insertSong(Song song) {
        String sql = """
                INSERT INTO songs (song_artist, song_title, song_album, song_year, song_genre, song_file_path)
                VALUES (?, ?, ?, ?, ?, ?);
                """;

        try (
                Connection connection = connect();
                PreparedStatement preparedStatement = connection.prepareStatement(sql);
                ) {
            preparedStatement.setString(1, song.getSongArtist());
            preparedStatement.setString(2, song.getSongTitle());
            preparedStatement.setString(3, song.getSongAlbum());
            preparedStatement.setInt(4, song.getSongYear());
            preparedStatement.setString(5, song.getSongGenre());
            preparedStatement.setString(6, song.getSongFilePath());

            preparedStatement.executeUpdate();
        } catch (SQLException exc) {
            throw new RuntimeException("Could not add song into database.", exc);
        }
    }

    public List<Song> getAllSongs() {
        List<Song> songsList = new ArrayList<>();

        String sql = """
                SELECT * FROM songs
                ORDER BY song_artist, song_title;
                """;

        try (
                Connection connection = connect();
                PreparedStatement preparedStatement = connection.prepareStatement(sql);
                ResultSet resultSet = preparedStatement.executeQuery();
                ) {

            while (resultSet.next()) {
                Song song = new Song(
                        resultSet.getInt("song_id"),
                        resultSet.getString("song_artist"),
                        resultSet.getString("song_title"),
                        resultSet.getString("song_album"),
                        resultSet.getInt("song_year"),
                        resultSet.getString("song_genre"),
                        resultSet.getString("song_file_path")
                );

                songsList.add(song);
            }
            return songsList;
        } catch (SQLException exc) {
            throw new RuntimeException("Could not load songs from database.", exc);
        }
    }

    public List<Song> getSongsByCriteria(String songArtist, String songTitle, String songAlbum, String songYear, String songGenre) {
        List<Song> songsList = new ArrayList<>();
        List<Object> parameters = new ArrayList<>();

        StringBuilder sql = new StringBuilder("""
                SELECT * FROM songs
                WHERE 1 = 1
                """);

        if (!songArtist.isBlank()) {
            sql.append(" AND song_artist LIKE ?");
            parameters.add("%" + songArtist.trim() + "%");
        }

        if (!songTitle.isBlank()) {
            sql.append(" AND song_title LIKE ?");
            parameters.add("%" + songTitle.trim() + "%");
        }

        if (!songAlbum.isBlank()) {
            sql.append(" AND song_album LIKE ?");
            parameters.add("%" + songAlbum.trim() + "%");
        }

        if (!songYear.isBlank()) {
            sql.append(" AND song_year = ?");
            parameters.add(Integer.parseInt(songYear.trim()));
        }

        if (!songGenre.isBlank()) {
            sql.append(" AND song_genre = ?");
            parameters.add(songGenre);
        }

        sql.append(" ORDER BY song_artist, song_title");

        try (
                Connection connection = connect();
                PreparedStatement preparedStatement = connection.prepareStatement(sql.toString());
                ) {

            int parameterIndex = 1;
            for (Object parameter : parameters) {
                preparedStatement.setObject(parameterIndex++, parameter);
            }

            try (ResultSet resultSet = preparedStatement.executeQuery()) {
                while (resultSet.next()) {
                    Song song = new Song(
                            resultSet.getInt("song_id"),
                            resultSet.getString("song_artist"),
                            resultSet.getString("song_title"),
                            resultSet.getString("song_album"),
                            resultSet.getInt("song_year"),
                            resultSet.getString("song_genre"),
                            resultSet.getString("song_file_path")
                    );
                    songsList.add(song);
                }
            }
            return songsList;
        } catch (SQLException exc) {
            throw new RuntimeException("Could not load songs from database.", exc);
        }
    }

    public boolean songFilePathExists(String songFilePath) {
        String sql = """
                SELECT 1 FROM songs
                WHERE song_file_path = ?;
                """;

        try (
                Connection connection = connect();
                PreparedStatement preparedStatement = connection.prepareStatement(sql);
                ) {
            preparedStatement.setString(1, songFilePath);

            try (ResultSet resultSet = preparedStatement.executeQuery()) {
                return resultSet.next();
            }
        } catch (SQLException exc) {
            throw new RuntimeException("Could not check for duplicates.", exc);
        }
    }

    public void updateSong(Song song) {
        String sql = """
                UPDATE songs
                SET song_artist = ?, song_title = ?, song_album = ?, song_year = ?, song_genre = ?, song_file_path = ?
                WHERE song_id = ?;
                """;

        try (
                Connection connection = connect();
                PreparedStatement preparedStatement = connection.prepareStatement(sql);
                ) {
            preparedStatement.setString(1, song.getSongArtist());
            preparedStatement.setString(2, song.getSongTitle());
            preparedStatement.setString(3, song.getSongAlbum());
            preparedStatement.setInt(4, song.getSongYear());
            preparedStatement.setString(5, song.getSongGenre());
            preparedStatement.setString(6, song.getSongFilePath());
            preparedStatement.setInt(7, song.getSongId());

            preparedStatement.executeUpdate();
        } catch (SQLException exc) {
            throw new RuntimeException("Could not update song in database.", exc);
        }
    }

    public void deleteSongById(int songId) {
        String sql = """
                DELETE FROM songs
                WHERE song_id = ?;
                """;

        try (
                Connection connection = connect();
                PreparedStatement preparedStatement = connection.prepareStatement(sql);
                ) {
            preparedStatement.setInt(1, songId);
            preparedStatement.executeUpdate();
        } catch (SQLException exc) {
            throw new RuntimeException("Could not delete song from database.", exc);
        }
    }
}
