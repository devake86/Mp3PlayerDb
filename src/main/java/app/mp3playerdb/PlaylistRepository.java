package app.mp3playerdb;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class PlaylistRepository {

    /*
        DATABASE CONFIGURATION
     */

    private static final String DB_FOLDER = "db";
    private static final String DB_URL = "jdbc:sqlite:" + DB_FOLDER + "/mp3playerdb.db";

    /*
        DATABASE INITIALIZATION
     */

    public PlaylistRepository() {
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
                CREATE TABLE IF NOT EXISTS playlist_songs (
                    song_id INTEGER PRIMARY KEY,
                    playlist_position INTEGER NOT NULL,
                    
                    FOREIGN KEY (song_id) REFERENCES songs(song_id) ON DELETE CASCADE
                );
                """;

        try (
                Connection connection = connect();
                Statement statement = connection.createStatement();
                ) {
            statement.execute(sql);
        } catch (SQLException exc) {
            throw new RuntimeException("Could not create table 'playlist_songs'.", exc);
        }
    }

    /*
        PLAYLIST DATABASE OPERATIONS
     */

    public void addSongToPlaylist(int songId) {
        String sql = """
                INSERT INTO playlist_songs (song_id, playlist_position)
                VALUES (?, COALESCE((SELECT MAX(playlist_position) + 1 FROM playlist_songs), 1));
                """;

        try (
                Connection connection = connect();
                PreparedStatement preparedStatement = connection.prepareStatement(sql);
                ) {
            preparedStatement.setInt(1, songId);
            preparedStatement.executeUpdate();
        } catch (SQLException exc) {
            throw new RuntimeException("Could not add song to playlist.", exc);
        }
    }

    public boolean songExistsInPlaylist(int songId) {
        String sql = """
                SELECT 1 FROM playlist_songs WHERE song_id = ?;
                """;

        try (
                Connection connection = connect();
                PreparedStatement preparedStatement = connection.prepareStatement(sql);
                ) {
            preparedStatement.setInt(1, songId);

            try (ResultSet resultSet = preparedStatement.executeQuery()) {
                return resultSet.next();
            }
        } catch (SQLException exc) {
            throw new RuntimeException("Could not check if song already exists in playlist.", exc);
        }
    }

    public List<PlaylistEntry> getAllPlaylistEntries() {
        List<PlaylistEntry> playlistEntries = new ArrayList<>();

        String sql = """
                SELECT
                    playlist_songs.playlist_position,
                    songs.song_id,
                    songs.song_artist,
                    songs.song_title,
                    songs.song_album,
                    songs.song_year,
                    songs.song_genre,
                    songs.song_file_path
                FROM playlist_songs
                JOIN songs ON playlist_songs.song_id = songs.song_id
                ORDER BY playlist_songs.playlist_position;
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

                PlaylistEntry playlistEntry = new PlaylistEntry(resultSet.getInt("playlist_position"), song);

                playlistEntries.add(playlistEntry);
            }

            return playlistEntries;
        } catch  (SQLException exc) {
            throw new RuntimeException("Could not load playlist.", exc);
        }
    }

    public void removeSongFromPlaylist(int songId) {
        String sql = """
                DELETE FROM playlist_songs
                WHERE song_id = ?;
                """;

        Connection connection = null;

        try {
            connection = connect();
            connection.setAutoCommit(false);

            try (
                    PreparedStatement preparedStatement = connection.prepareStatement(sql)
                    ) {
                preparedStatement.setInt(1, songId);
                preparedStatement.executeUpdate();
            }

            normalizePlaylistPositions(connection);
            connection.commit();
        } catch (SQLException exc) {
            if (connection != null) {
                try {
                    connection.rollback();
                } catch (SQLException rollbackException) {
                    exc.addSuppressed(rollbackException);
                }
            }

            throw new RuntimeException("Could not remove song from playlist.", exc);
        } finally {
            if (connection != null) {
                try {
                    connection.close();
                } catch (SQLException closeException) {
                    throw new RuntimeException("Could not close database connection.", closeException);
                }
            }
        }
    }

    /*
        PLAYLIST ORDER
     */

    public void moveSongUp(int songId) {
        moveSong(songId, true);
    }

    public void moveSongDown(int songId) {
        moveSong(songId, false);
    }

    private void moveSong(int songId, boolean moveUp) {
        String currentPositionSql = """
                SELECT playlist_position
                FROM playlist_songs
                WHERE song_id = ?;
                """;

        String neighbourSql;

        if (moveUp) {neighbourSql = """
                        SELECT song_id, playlist_position
                        FROM playlist_songs
                        WHERE playlist_position < ?
                        ORDER BY playlist_position DESC
                        LIMIT 1;
                        """;
        } else {
            neighbourSql = """
                SELECT song_id, playlist_position
                FROM playlist_songs
                WHERE playlist_position > ?
                ORDER BY playlist_position ASC
                LIMIT 1;
                """;
        }

        String swapSql = """
                    UPDATE playlist_songs
                    SET playlist_position =
                        CASE
                            WHEN song_id = ? THEN ?
                            WHEN song_id = ? THEN ?
                            ELSE playlist_position
                        END
                    WHERE song_id IN (?, ?);
                    """;

        Connection connection = null;

        try {
            connection = connect();
            connection.setAutoCommit(false);

            int currentPosition;

            try (
                    PreparedStatement statement = connection.prepareStatement(currentPositionSql)
            ) {
                statement.setInt(1, songId);

                try (ResultSet resultSet = statement.executeQuery()) {
                    if (!resultSet.next()) {
                        connection.rollback();
                        return;
                    }

                    currentPosition = resultSet.getInt("playlist_position");
                }
            }

            int neighbourSongId;
            int neighbourPosition;

            try (
                    PreparedStatement statement = connection.prepareStatement(neighbourSql)
            ) {
                statement.setInt(1, currentPosition);

                try (ResultSet resultSet = statement.executeQuery()) {
                    if (!resultSet.next()) {
                        connection.rollback();
                        return;
                    }

                    neighbourSongId = resultSet.getInt("song_id");

                    neighbourPosition = resultSet.getInt("playlist_position");
                }
            }

            try (
                    PreparedStatement statement = connection.prepareStatement(swapSql)
            ) {
                statement.setInt(1, songId);
                statement.setInt(2, neighbourPosition);

                statement.setInt(3, neighbourSongId);
                statement.setInt(4, currentPosition);

                statement.setInt(5, songId);
                statement.setInt(6, neighbourSongId);

                statement.executeUpdate();
            }

            connection.commit();
        } catch (SQLException exc) {
            if (connection != null) {
                try {
                    connection.rollback();
                } catch (SQLException rollbackException) {
                    exc.addSuppressed(rollbackException);
                }
            }

            throw new RuntimeException( "Could not move playlist song.", exc);
        } finally {
            if (connection != null) {
                try {
                    connection.close();
                } catch (SQLException closeException) {
                    throw new RuntimeException("Could not close database connection.", closeException);
                }
            }
        }
    }

    /*
        PLAYLIST POSITION NORMALIZATION
     */

    public void normalizePositions() {
        Connection connection = null;

        try {
            connection = connect();
            connection.setAutoCommit(false);

            normalizePlaylistPositions(connection);

            connection.commit();
        } catch (SQLException exc) {
            if (connection != null) {
                try {
                    connection.rollback();
                } catch (SQLException rollbackException) {
                    exc.addSuppressed(rollbackException);
                }
            }

            throw new RuntimeException("Could not normalize playlist positions.", exc);
        } finally {
            if (connection != null) {
                try {
                    connection.close();
                } catch (SQLException closeException) {
                    throw new RuntimeException("Could not close database connection.", closeException);
                }
            }
        }
    }

    private void normalizePlaylistPositions(Connection connection) {
        String selectSql = """
                SELECT song_id FROM playlist_songs
                ORDER BY playlist_position;
                """;

        String updateSql = """
                UPDATE playlist_songs SET playlist_position = ?
                WHERE song_id = ?;
                """;

        try (
                PreparedStatement selectStatement = connection.prepareStatement(selectSql);
                ResultSet resultSet = selectStatement.executeQuery();
                PreparedStatement updateStatement = connection.prepareStatement(updateSql)
                ) {
            int newPosition = 1;

            while (resultSet.next()) {
                int songId = resultSet.getInt("song_id");

                updateStatement.setInt(1, newPosition);
                updateStatement.setInt(2, songId);
                updateStatement.addBatch();

                newPosition++;
            }

            updateStatement.executeBatch();
        } catch (SQLException exc) {
            throw new RuntimeException("Could not update playlist.", exc);
        }
    }
}
