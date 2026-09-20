package app.mp3playerdb;

import com.mpatric.mp3agic.ID3v1;
import com.mpatric.mp3agic.Mp3File;
import javafx.application.Platform;
import javafx.beans.property.ReadOnlyObjectWrapper;
import javafx.beans.property.ReadOnlyStringWrapper;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.input.Dragboard;
import javafx.scene.input.TransferMode;
import javafx.scene.layout.GridPane;
import javafx.scene.media.MediaPlayer;
import javafx.scene.text.Font;
import javafx.scene.text.Text;
import javafx.stage.FileChooser;
import javafx.stage.Window;
import javafx.css.PseudoClass;
import java.io.File;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.function.Function;
import java.util.prefs.Preferences;

public class MainController {

    /*
        PREFERENCES
     */

    private static final String PREFERENCES_SETTINGS_NODE = "Mp3PlayerDb/settings";
    private static final String PREFERENCES_THEME_SETTING_KEY = "themeSetting";
    private static final int PREFERENCES_LIGHT_THEME = 1;
    private static final int PREFERENCES_DARK_THEME = 2;
    private static final int PREFERENCES_THEME_NOT_SET = -1;
    private static final String PREFERENCES_DARK_THEME_CSS = "/css/dark-theme.css";
    private static final String PREFERENCES_LIGHT_THEME_CSS = "/css/light-theme.css";
    private static final String PREFERENCES_LAST_SONG_ID_KEY = "lastSongId";
    private static final int PREFERENCES_NO_SONG_ID = -1;
    private static final String PREFERENCES_VOLUME_KEY = "volume";
    private static final double PREFERENCES_DEFAULT_VOLUME = 50.0;

    /*
        UI CONSTANTS
     */

    private static final PseudoClass LOADED_SONG_PSEUDO_CLASS =  PseudoClass.getPseudoClass("loaded-song");

    private static final String GENRE_PROMPT = "Select Genre";

    private static final double LOWERED_BUTTON_OPACITY = 0.65;
    private static final double ENABLED_BUTTON_OPACITY = 1.0;

    /*
        APPLICATION STATE AND DEPENDENCIES
     */

    private Preferences preferences;

    private final SongRepository songRepository = new SongRepository();

    private final PlaylistRepository playlistRepository = new PlaylistRepository();

    private final Mp3Player mp3Player = new Mp3Player();

    private boolean timeSliderInUse;

    // Null indicates that no song is currently loaded
    private Integer loadedSongId;

    /*
        ROOT LAYOUT
     */

    @FXML
    private GridPane rootLayout;

    /*
        SONG INPUT CONTROLS
     */

    @FXML
    private TextField songArtistTextField;
    @FXML
    private TextField songTitleTextField;
    @FXML
    private TextField songAlbumTextField;
    @FXML
    private TextField songYearTextField;
    @FXML
    private ComboBox<String> songGenreComboBox;
    @FXML
    private TextField songFilePathTextField;
    @FXML
    private Button selectFileButton;

    /*
        SONG ACTION CONTROLS
     */

    @FXML
    private Button clearAndShowAllButton;
    @FXML
    private Button createButton;
    @FXML
    private Button searchButton;
    @FXML
    private Button updateButton;
    @FXML
    private Button deleteButton;
    @FXML
    private Button loadAndPlaySelectedSongButton;

    /*
        MEDIA PLAYER CONTROLS
     */

    @FXML
    private Label selectedSongLabel;
    @FXML
    private Label timeLabel;
    @FXML
    private Label volumeLabel;
    @FXML
    private Button previousButton;
    @FXML
    private Button playPauseButton;
    @FXML
    private Button nextButton;
    @FXML
    private Slider timeSlider;
    @FXML
    private Slider volumeSlider;

    /*
        PLAYLIST ACTION CONTROLS
     */

    @FXML
    private Button posUpButton;
    @FXML
    private Button posDownButton;
    @FXML
    private Button addButton;
    @FXML
    private Button removeButton;

    /*
        THEME CONTROLS
     */

    @FXML
    private Button lightThemeButton;
    @FXML
    private Button darkThemeButton;

    /*
        SONG TABLE
     */

    @FXML
    private Label songsLabel;
    @FXML
    private TableView<Song> songsTableView;
    @FXML
    private TableColumn<Song, Integer> songIdColumn;
    @FXML
    private TableColumn<Song, String> songArtistColumn;
    @FXML
    private TableColumn<Song, String> songTitleColumn;
    @FXML
    private TableColumn<Song, String> songAlbumColumn;
    @FXML
    private TableColumn<Song, Integer> songYearColumn;
    @FXML
    private TableColumn<Song, String> songGenreColumn;
    @FXML
    private TableColumn<Song, String> songFilePathColumn;

    /*
        PLAYLIST TABLE
     */

    @FXML
    private Label playlistLabel;
    @FXML
    private TableView<PlaylistEntry> playlistSongsTableView;
    @FXML
    private TableColumn<PlaylistEntry, Integer> playlistPositionColumn;
    @FXML
    private TableColumn<PlaylistEntry, String> playlistSongArtistColumn;
    @FXML
    private TableColumn<PlaylistEntry, String> playlistSongTitleColumn;

    /*
        INITIALIZATION
     */

    @FXML
    public void initialize() {
        initUiSettings();
        initUi();
        applyTheme(preferences);
        initButtonHandler();

        showAllSongs();
        showAllPlaylistSongs();
        restoreLastPlaylistSelection();

        handleSongSelection();
    }

    private void initUiSettings() {
        preferences = Preferences.userRoot().node(PREFERENCES_SETTINGS_NODE);

        if (preferences.getInt(PREFERENCES_THEME_SETTING_KEY, PREFERENCES_THEME_NOT_SET) == PREFERENCES_THEME_NOT_SET) {
            preferences.putInt(PREFERENCES_THEME_SETTING_KEY, PREFERENCES_DARK_THEME);
        }
    }

    private void initUi() {
        songArtistTextField.setVisible(true);
        songArtistTextField.setManaged(true);

        songTitleTextField.setVisible(true);
        songTitleTextField.setManaged(true);

        songAlbumTextField.setVisible(true);
        songAlbumTextField.setManaged(true);

        songYearTextField.setVisible(true);
        songYearTextField.setManaged(true);

        songGenreComboBox.getItems().addAll(
                GENRE_PROMPT,
                "Blues",
                "Classical",
                "Country",
                "Electronic",
                "Folk",
                "Hip-Hop",
                "Jazz",
                "Metal",
                "Pop",
                "R&B",
                "Reggae",
                "Rock",
                "Soul",
                "Other"
        );
        songGenreComboBox.getSelectionModel().selectFirst();
        songGenreComboBox.setVisible(true);
        songGenreComboBox.setManaged(true);

        songFilePathTextField.setVisible(true);
        songFilePathTextField.setManaged(true);

        selectFileButton.setVisible(true);
        selectFileButton.setManaged(true);

        rootLayout.setOnDragOver(event -> {
            Dragboard dragboard = event.getDragboard();

            if (dragboard.hasFiles() && containsMp3File(dragboard.getFiles())) {
                event.acceptTransferModes(TransferMode.COPY);
            }

            event.consume();
        });

        rootLayout.setOnDragDropped(event -> {
            Dragboard dragboard = event.getDragboard();

            boolean completed = false;

            if (dragboard.hasFiles()) {
                File mp3File = findFirstMp3File(dragboard.getFiles());

                if (mp3File != null) {
                    clearInputFields();
                    handleSelectMp3File(mp3File);
                    completed = true;
                }
            }

            event.setDropCompleted(completed);
            event.consume();
        });

        clearAndShowAllButton.setVisible(true);
        clearAndShowAllButton.setManaged(true);

        createButton.setVisible(true);
        createButton.setManaged(true);

        searchButton.setVisible(true);
        searchButton.setManaged(true);

        updateButton.setVisible(true);
        updateButton.setManaged(true);

        deleteButton.setVisible(true);
        deleteButton.setManaged(true);

        double savedVolume = preferences.getDouble(PREFERENCES_VOLUME_KEY, PREFERENCES_DEFAULT_VOLUME);

        volumeSlider.setValue(savedVolume);
        volumeLabel.setText(String.valueOf((int) savedVolume));

        mp3Player.setVolume(savedVolume / 100.0);

        volumeSlider.valueProperty().addListener((observable, oldValue, newValue) -> {
            double volume = newValue.doubleValue() / 100.0;

            mp3Player.setVolume(volume);

            volumeLabel.setText(String.valueOf(newValue.intValue()));

            preferences.putDouble(PREFERENCES_VOLUME_KEY, newValue.doubleValue());
        });

        handleTimeSlider();

        lightThemeButton.setVisible(true);
        lightThemeButton.setManaged(true);

        darkThemeButton.setVisible(true);
        darkThemeButton.setManaged(true);

        if (preferences.getInt(PREFERENCES_THEME_SETTING_KEY, PREFERENCES_DARK_THEME) == PREFERENCES_DARK_THEME) {
            lightThemeButton.setOpacity(LOWERED_BUTTON_OPACITY);
            darkThemeButton.setOpacity(ENABLED_BUTTON_OPACITY);
        } else {
            lightThemeButton.setOpacity(ENABLED_BUTTON_OPACITY);
            darkThemeButton.setOpacity(LOWERED_BUTTON_OPACITY);
        }

        songIdColumn.setCellValueFactory(cellData -> new ReadOnlyObjectWrapper<>(cellData.getValue().getSongId()));
        songArtistColumn.setCellValueFactory(cellData -> new ReadOnlyStringWrapper(cellData.getValue().getSongArtist()));
        songTitleColumn.setCellValueFactory(cellData -> new ReadOnlyStringWrapper(cellData.getValue().getSongTitle()));
        songAlbumColumn.setCellValueFactory(cellData -> new ReadOnlyStringWrapper(cellData.getValue().getSongAlbum()));
        songYearColumn.setCellValueFactory(cellData -> new ReadOnlyObjectWrapper<>(cellData.getValue().getSongYear()));
        songGenreColumn.setCellValueFactory(cellData -> new ReadOnlyStringWrapper(cellData.getValue().getSongGenre()));
        songFilePathColumn.setCellValueFactory(cellData -> new ReadOnlyStringWrapper(cellData.getValue().getSongFilePath()));

        songsTableView.setColumnResizePolicy(TableView.UNCONSTRAINED_RESIZE_POLICY);

        songsLabel.setVisible(true);
        songsLabel.setManaged(true);

        songsTableView.setVisible(true);
        songsTableView.setManaged(true);

        songsTableView.setRowFactory(tableView -> new TableRow<>() {
            @Override
            protected void updateItem(Song song, boolean empty) {
                super.updateItem(song, empty);

                boolean loadedSong = !empty && song != null && isSongLoaded(song);

                pseudoClassStateChanged(LOADED_SONG_PSEUDO_CLASS, loadedSong);
            }
        });

        playlistPositionColumn.setCellValueFactory(cellData -> new ReadOnlyObjectWrapper<>(cellData.getValue().getPlaylistPosition()));
        playlistSongArtistColumn.setCellValueFactory(cellData -> new ReadOnlyStringWrapper(cellData.getValue().getSongArtist()));
        playlistSongTitleColumn.setCellValueFactory(cellData -> new ReadOnlyStringWrapper(cellData.getValue().getSongTitle()));

        playlistPositionColumn.setSortable(false);
        playlistSongArtistColumn.setSortable(false);
        playlistSongTitleColumn.setSortable(false);

        playlistSongsTableView.setColumnResizePolicy(TableView.UNCONSTRAINED_RESIZE_POLICY);

        playlistLabel.setVisible(true);
        playlistLabel.setManaged(true);

        playlistSongsTableView.setVisible(true);
        playlistSongsTableView.setManaged(true);

        playlistSongsTableView.setRowFactory(tableView -> {
            TableRow<PlaylistEntry> row = new TableRow<>() {
                @Override
                protected void updateItem( PlaylistEntry playlistEntry, boolean empty) {
                    super.updateItem(playlistEntry, empty);

                    boolean loadedSong = !empty && playlistEntry != null && isSongLoaded(playlistEntry.getSong());

                    pseudoClassStateChanged(LOADED_SONG_PSEUDO_CLASS, loadedSong);
                }
            };

            row.setOnMouseClicked(event -> {
                if (event.getClickCount() == 2 && !row.isEmpty()) {
                    PlaylistEntry playlistEntry = row.getItem();

                    Song song = playlistEntry.getSong();

                    if (isSongLoaded(song) && mp3Player.getMediaPlayer() != null && mp3Player.getMediaPlayer().getStatus() == MediaPlayer.Status.PLAYING) {
                        return;
                    }

                    if (!isSongLoaded(song)) {
                        loadSong(song);
                    }

                    mp3Player.play();
                }
            });

            return row;
        });
    }

    private void initButtonHandler() {
        selectFileButton.setOnAction(event -> handleSelectFileButton());

        clearAndShowAllButton.setOnAction(event -> handleClearTextFieldsButton());

        createButton.setOnAction(event -> handleCreateButton());
        searchButton.setOnAction(event -> handleSearchButton());
        updateButton.setOnAction(event -> handleUpdateButton());
        deleteButton.setOnAction(event -> handleDeleteButton());

        loadAndPlaySelectedSongButton.setOnAction(event -> handleLoadAndPlaySelectedSongButton());

        lightThemeButton.setOnAction(event -> handleLightThemeButton());
        darkThemeButton.setOnAction(event -> handleDarkThemeButton());

        previousButton.setOnAction(event -> handlePreviousButton());
        playPauseButton.setOnAction(event -> handlePlayPauseButton());
        nextButton.setOnAction(event -> handleNextButton());

        posUpButton.setOnAction(event -> handlePosUpButton());
        posDownButton.setOnAction(event -> handlePosDownButton());
        addButton.setOnAction(event -> handleAddButton());
        removeButton.setOnAction(event -> handleRemoveButton());
    }

    /*
        FILE SELECTION AND METADATA
     */

    private void handleSelectFileButton() {
        FileChooser fileChooser = new FileChooser();

        fileChooser.setTitle("Select MP3-File");

        fileChooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("MP3 File", "*.mp3"));

        File selectedFile = fileChooser.showOpenDialog(selectFileButton.getScene().getWindow());

        if (selectedFile != null) {
            handleSelectMp3FileFromFileChooser(selectedFile);
        }
    }

    private void handleSelectMp3FileFromFileChooser(File mp3File) {
        ButtonType keepInputButton = new ButtonType("Keep Input", ButtonBar.ButtonData.LEFT);
        ButtonType importMetadataButton = new ButtonType("Import Metadata", ButtonBar.ButtonData.OTHER);

        Alert metadataConfirmation = new Alert(
                Alert.AlertType.CONFIRMATION,
                "Do you want to keep your current input or\nimport the MP3 metadata?",
                keepInputButton,
                importMetadataButton
        );

        metadataConfirmation.setTitle("Import Metadata");
        metadataConfirmation.setHeaderText(null);
        centerAlertOnMainWindow(metadataConfirmation);

        metadataConfirmation.showAndWait().ifPresent(buttonType -> {
            if (buttonType == importMetadataButton) {
                clearInputFields();
                handleSelectMp3File(mp3File);
            } else if (buttonType == keepInputButton) {
                songFilePathTextField.setText(mp3File.getAbsolutePath());
            }
        });
    }

    private void handleSelectMp3File(File mp3File) {
        songFilePathTextField.setText(mp3File.getAbsolutePath());

        fillInputFieldsFromMetadata(mp3File);
    }

    private boolean containsMp3File(List<File> files) {
        return findFirstMp3File(files) != null;
    }

    private File findFirstMp3File(List<File> files) {
        for (File file : files) {
            String fileName = file.getName().toLowerCase(Locale.ROOT);

            if (file.isFile() && fileName.endsWith(".mp3")) {
                return file;
            }
        }

        return null;
    }

    private void fillInputFieldsFromMetadata(File mp3File) {
        try {
            Mp3File metadataFile = new Mp3File(mp3File.getAbsolutePath());

            ID3v1 metadataTag;

            if (metadataFile.hasId3v2Tag()) {
                metadataTag = metadataFile.getId3v2Tag();
            } else if (metadataFile.hasId3v1Tag()) {
                metadataTag = metadataFile.getId3v1Tag();
            } else {
                return;
            }

            String metadataArtist = metadataTag.getArtist();
            String metadataTitle = metadataTag.getTitle();
            String metadataAlbum = metadataTag.getAlbum();
            String metadataYear = metadataTag.getYear();
            String metadataGenre = metadataTag.getGenreDescription();

            fillTextFieldIfEmpty(songArtistTextField, metadataArtist);
            fillTextFieldIfEmpty(songTitleTextField, metadataTitle);
            fillTextFieldIfEmpty(songAlbumTextField, metadataAlbum);
            fillTextFieldIfEmpty(songYearTextField, metadataYear);
            fillGenreIfEmpty(metadataGenre);
        } catch (Exception exc){
            showError("Could not read MP3 metadata.");
            exc.printStackTrace();
        }
    }

    private void fillTextFieldIfEmpty(TextField textField, String metaDataValue) {
        if (textField.getText().isBlank() && metaDataValue != null && !metaDataValue.isBlank()) {
            textField.setText(metaDataValue.trim());
        }
    }

    private void fillGenreIfEmpty(String metaDataGenre) {
        boolean noGenreSelected = songGenreComboBox.getValue() == null || songGenreComboBox.getValue().equals(GENRE_PROMPT);

        if (!noGenreSelected) {
            return;
        }

        if (metaDataGenre == null || metaDataGenre.isBlank()) {
            return;
        }

        for (String genre : songGenreComboBox.getItems()) {
            if (genre.equalsIgnoreCase(metaDataGenre.trim())) {
                songGenreComboBox.setValue(genre);
                return;
            }
        }
    }

    /*
        SONG MANAGEMENT
     */

    private void handleClearTextFieldsButton() {
        clearInputFields();
        showAllSongs();
    }

    private void handleCreateButton() {
        if (!validInputs()) {
            showError("Song information not valid");
            return;
        }

        String filePath = songFilePathTextField.getText().trim();

        if (songRepository.songFilePathExists(filePath)) {
            showError("Song already exists in database.");
            return;
        }

        Song song = createSongFromTextFields();
        songRepository.insertSong(song);

        showAllSongs();
        clearInputFields();
    }

    private void handleSearchButton() {
        String songArtist = songArtistTextField.getText();
        String songTitle = songTitleTextField.getText();
        String songAlbum = songAlbumTextField.getText();
        String songYear = songYearTextField.getText();
        String songGenre = songGenreComboBox.getValue();

        if (!songYear.isBlank()) {
            try{
                Integer.parseInt(songYear);
            } catch (NumberFormatException exc) {
                showError("Invalid Year");
                return;
            }
        }

        if (songGenre == null || songGenre.equals(GENRE_PROMPT)) {
            songGenre = "";
        }

        List<Song> searchResults = songRepository.getSongsByCriteria(songArtist, songTitle, songAlbum, songYear, songGenre);

        songsTableView.getItems().setAll(searchResults);
        resizeAllColumnsToContent();
    }

    private void handleUpdateButton() {
        Song selectedSong = songsTableView.getSelectionModel().getSelectedItem();

        if (selectedSong == null) {
            showError("A song must be selected");
            return;
        }

        if (!validInputs()) {
            showError("Song information not valid");
            return;
        }

        Song updatedSong = new Song(
                selectedSong.getSongId(),
                songArtistTextField.getText().trim(),
                songTitleTextField.getText().trim(),
                songAlbumTextField.getText().trim(),
                Integer.parseInt(songYearTextField.getText().trim()),
                songGenreComboBox.getValue(),
                songFilePathTextField.getText().trim()
        );

        songRepository.updateSong(updatedSong);

        showAllSongs();
        clearInputFields();
    }

    private void handleDeleteButton() {
        Song selectedSong = songsTableView.getSelectionModel().getSelectedItem();

        if (selectedSong == null) {
            showError("A song must be selected");
            return;
        }

        ButtonType cancelButtonType = new ButtonType("Cancel", ButtonBar.ButtonData.LEFT);
        ButtonType deleteButtonType = new ButtonType("Delete", ButtonBar.ButtonData.RIGHT);

        Alert confirmation = new Alert(
                Alert.AlertType.CONFIRMATION,
                "Delete selected song from database?\n\n"
                        + selectedSong.getSongArtist() + " - " + selectedSong.getSongTitle(),
                cancelButtonType, deleteButtonType
        );

        confirmation.setTitle("Confirm Delete");
        confirmation.setHeaderText(null);

        centerAlertOnMainWindow(confirmation);

        confirmation.showAndWait().ifPresent(buttonType -> {
            if (buttonType == deleteButtonType) {

                int deletedSongId = selectedSong.getSongId();
                songRepository.deleteSongById(deletedSongId);

                playlistRepository.normalizePositions();

                int savedSongId = preferences.getInt(PREFERENCES_LAST_SONG_ID_KEY, PREFERENCES_NO_SONG_ID);

                if (savedSongId == deletedSongId) {
                    preferences.remove(PREFERENCES_LAST_SONG_ID_KEY);
                }

                showAllSongs();
                showAllPlaylistSongs();
                clearInputFields();
            }
        });
    }

    private Song createSongFromTextFields() {
        String songArtist = songArtistTextField.getText().trim();
        String songTitle = songTitleTextField.getText().trim();
        String songAlbum = songAlbumTextField.getText().trim();
        int songYear = Integer.parseInt(songYearTextField.getText().trim());
        String songGenre = songGenreComboBox.getValue();
        String songFilePath = songFilePathTextField.getText().trim();

        return new Song(songArtist, songTitle, songAlbum, songYear, songGenre, songFilePath);
    }

    private boolean validInputs() {
        if (songArtistTextField.getText().isBlank()) {
            return false;
        }

        if (songTitleTextField.getText().isBlank()) {
            return false;
        }

        if (songAlbumTextField.getText().isBlank()) {
            return false;
        }

        try {
            Integer.parseInt(songYearTextField.getText().trim());
        } catch (NumberFormatException exc) {
            return false;
        }

        if (songGenreComboBox.getValue() == null || songGenreComboBox.getValue().equals(GENRE_PROMPT)) {
            return false;
        }

        if (songFilePathTextField.getText().isBlank()) {
            return false;
        }

        return true;
    }

    private void clearInputFields() {
        songArtistTextField.clear();
        songTitleTextField.clear();
        songAlbumTextField.clear();
        songYearTextField.clear();
        songGenreComboBox.getSelectionModel().selectFirst();
        songFilePathTextField.clear();

        songsTableView.getSelectionModel().clearSelection();
    }

    private void showSongInInputFields(Song song) {
        songArtistTextField.setText(song.getSongArtist());
        songTitleTextField.setText(song.getSongTitle());
        songAlbumTextField.setText(song.getSongAlbum());
        songYearTextField.setText(String.valueOf(song.getSongYear()));
        songGenreComboBox.setValue(song.getSongGenre());
        songFilePathTextField.setText(song.getSongFilePath());
    }

    private void showAllSongs() {
        songsTableView.getItems().setAll(songRepository.getAllSongs());

        resizeAllColumnsToContent();
    }

    private void handleSongSelection() {
        songsTableView.getSelectionModel().selectedItemProperty().addListener((observable, oldSong, selectedSong) -> {
            if (selectedSong != null) {
                showSongInInputFields(selectedSong);
            }
        });
    }

    /*
        MEDIA PLAYER
     */

    private void handleLoadAndPlaySelectedSongButton() {
        Song selectedSong = songsTableView.getSelectionModel().getSelectedItem();

        if (selectedSong == null) {
            showError("A song must be selected");
            return;
        }

        if (!isSongLoaded(selectedSong)) {
            loadSong(selectedSong);
        }

        mp3Player.play();
    }

    private void handlePlayPauseButton() {
        MediaPlayer mediaPlayer = mp3Player.getMediaPlayer();

        if (mediaPlayer != null) {
            if (mediaPlayer.getStatus() == MediaPlayer.Status.PLAYING) {
                mp3Player.pause();
            } else {
                mp3Player.play();
            }

            return;
        }

        PlaylistEntry selectedPlaylistEntry = playlistSongsTableView.getSelectionModel().getSelectedItem();

        if (selectedPlaylistEntry == null) {
            showError("A playlist song must be selected.");
            return;
        }

        Song selectedPlaylistSong = selectedPlaylistEntry.getSong();

        loadSong(selectedPlaylistSong);
        mp3Player.play();
    }

    private void handlePreviousButton() {
        List<PlaylistEntry> playlistEntries = playlistSongsTableView.getItems();

        if (playlistEntries.isEmpty()) {
            showError("Playlist is empty.");
            return;
        }

        int currentIndex = getLoadedPlaylistIndex();

        if (currentIndex == -1) {
            int selectedIndex = playlistSongsTableView.getSelectionModel().getSelectedIndex();

            loadAndPlayPlaylistEntry(selectedIndex >= 0 ? selectedIndex : 0);

            return;
        }

        int previousIndex = currentIndex - 1;

        if (previousIndex < 0) {
            previousIndex = playlistEntries.size() - 1;
        }

        loadAndPlayPlaylistEntry(previousIndex);
    }

    private void handleNextButton() {
        List<PlaylistEntry> playlistEntries = playlistSongsTableView.getItems();

        if (playlistEntries.isEmpty()) {
            showError("Playlist is empty.");
            return;
        }

        int currentIndex = getLoadedPlaylistIndex();

        if (currentIndex == -1) {
            int selectedIndex = playlistSongsTableView.getSelectionModel().getSelectedIndex();

            loadAndPlayPlaylistEntry(selectedIndex >= 0 ? selectedIndex : 0);

            return;
        }

        int nextIndex = currentIndex + 1;

        if (nextIndex >= playlistEntries.size()) {
            nextIndex = 0;
        }

        loadAndPlayPlaylistEntry(nextIndex);
    }

    private void loadSong(Song selectedSong) {
        File file = new File(selectedSong.getSongFilePath());

        if (!file.exists()) {
            showError("Song file does not exist");
            return;
        }

        mp3Player.loadMp3File(selectedSong.getSongFilePath());

        mp3Player.setVolume(volumeSlider.getValue() / 100.0);

        loadedSongId = selectedSong.getSongId();

        songsTableView.refresh();
        playlistSongsTableView.refresh();

        preferences.putInt(PREFERENCES_LAST_SONG_ID_KEY, loadedSongId);

        String selectedSongText = selectedSong.getSongArtist() + " - " + selectedSong.getSongTitle();
        selectedSongLabel.setText(selectedSongText);

        configureLoadedMedia();
    }

    private boolean isSongLoaded(Song song) {
        return song != null && loadedSongId != null && loadedSongId.equals(song.getSongId());
    }

    private void configureLoadedMedia() {
        MediaPlayer mediaPlayer = mp3Player.getMediaPlayer();

        if (mediaPlayer == null) {
            return;
        }

        mediaPlayer.statusProperty().addListener((observable, oldStatus, newStatus) -> {
            if (newStatus == MediaPlayer.Status.PLAYING) {
                playPauseButton.setText("⏸");
            } else {
                playPauseButton.setText("▶");
            }
        });

        mediaPlayer.setOnReady(() -> {
            double totalSeconds = mediaPlayer.getTotalDuration().toSeconds();

            timeSlider.setMin(0);
            timeSlider.setMax(totalSeconds);

            updateTimeLabel(0, totalSeconds);
        });

        mediaPlayer.setOnEndOfMedia(() -> {
            int playlistIndex = getLoadedPlaylistIndex();

            if (playlistIndex >= 0) {
                handleNextButton();
            }
        });

        mediaPlayer.currentTimeProperty().addListener((observable, oldTime, currentTime) -> {

            if (!timeSliderInUse) {
                timeSlider.setValue(currentTime.toSeconds());
            }

            updateTimeLabel(currentTime.toSeconds(), mediaPlayer.getTotalDuration().toSeconds());

        });
    }

    private void handleTimeSlider() {
        timeSlider.setOnMousePressed(event -> {
            timeSliderInUse = true;
            seekToMousePosition(event.getX());
        });

        timeSlider.setOnMouseDragged(event -> {
            seekToMousePosition(event.getX());
        });

        timeSlider.setOnMouseReleased(event -> {
            seekToMousePosition(event.getX());
            mp3Player.seekDuration(timeSlider.getValue());
            timeSliderInUse = false;
        });
    }

    private void seekToMousePosition(double mouseX) {
        double sliderWidth = timeSlider.getWidth();

        if (sliderWidth <= 0) {
            return;
        }

        double clickedPercentage = mouseX / sliderWidth;

        clickedPercentage = Math.max(0, Math.min(1, clickedPercentage));

        double selectedSeconds = timeSlider.getMin() + clickedPercentage * (timeSlider.getMax() - timeSlider.getMin());

        timeSlider.setValue(selectedSeconds);
    }

    private String formatTime(double totalSeconds) {
        int minutes = (int) totalSeconds / 60;
        int seconds = (int) totalSeconds % 60;

        return String.format("%d:%02d", minutes, seconds);
    }

    private void updateTimeLabel(double currentSeconds, double totalSeconds) {
        timeLabel.setText(formatTime(currentSeconds) + " / " + formatTime(totalSeconds));
    }

    /*
        PLAYLIST MANAGEMENT
     */

    private void handlePosUpButton() {
        PlaylistEntry selectedPlaylistEntry = playlistSongsTableView.getSelectionModel().getSelectedItem();

        if (selectedPlaylistEntry == null) {
            showError("A playlist song must be selected.");
            return;
        }

        int songId = selectedPlaylistEntry.getSong().getSongId();

        playlistRepository.moveSongUp(songId);

        showAllPlaylistSongs();
        selectPlaylistSongById(songId);
    }

    private void handlePosDownButton() {
        PlaylistEntry selectedPlaylistEntry = playlistSongsTableView.getSelectionModel().getSelectedItem();

        if (selectedPlaylistEntry == null) {
            showError("A playlist song must be selected.");
            return;
        }

        int songId = selectedPlaylistEntry.getSong().getSongId();

        playlistRepository.moveSongDown(songId);

        showAllPlaylistSongs();
        selectPlaylistSongById(songId);
    }

    private void handleAddButton() {
        Song selectedSong = songsTableView.getSelectionModel().getSelectedItem();

        if (selectedSong == null) {
            showError("A song must be selected.");
            return;
        }

        if (playlistRepository.songExistsInPlaylist(selectedSong.getSongId())) {
            showError("Song already exists in playlist.");
            return;
        }

        playlistRepository.addSongToPlaylist(selectedSong.getSongId());

        showAllPlaylistSongs();
    }

    private void handleRemoveButton() {
        int selectedIndex = playlistSongsTableView.getSelectionModel().getSelectedIndex();

        PlaylistEntry selectedPlaylistEntry = playlistSongsTableView.getSelectionModel().getSelectedItem();

        if (selectedPlaylistEntry == null) {
            showError("A playlist song must be selected.");
            return;
        }

        ButtonType cancelButtonType = new ButtonType("Cancel", ButtonBar.ButtonData.LEFT);
        ButtonType removeButtonType = new ButtonType("Remove", ButtonBar.ButtonData.RIGHT);

        Alert confirmation = new Alert(
                Alert.AlertType.CONFIRMATION,
                "Remove selected song from playlist?\n\n"
                        + selectedPlaylistEntry.getSongArtist() + " - " + selectedPlaylistEntry.getSongTitle(),
                cancelButtonType, removeButtonType
        );

        confirmation.setTitle("Confirm Remove");
        confirmation.setHeaderText(null);

        centerAlertOnMainWindow(confirmation);

        confirmation.showAndWait().ifPresent(buttonType -> {
            if (buttonType == removeButtonType) {
                int songId = selectedPlaylistEntry.getSong().getSongId();

                playlistRepository.removeSongFromPlaylist(songId);

                showAllPlaylistSongs();

                if (!playlistSongsTableView.getItems().isEmpty()) {
                    int lastIndex = playlistSongsTableView.getItems().size() - 1;

                    int newSelectedIndex = Math.min(selectedIndex, lastIndex);

                    playlistSongsTableView.getSelectionModel().clearAndSelect(newSelectedIndex);

                    playlistSongsTableView.scrollTo(newSelectedIndex);
                }
            }
        });
    }

    private void showAllPlaylistSongs() {
        playlistSongsTableView.getItems().setAll(playlistRepository.getAllPlaylistEntries());

        resizePlaylistColumnsToContent();
    }

    private int getLoadedPlaylistIndex() {
        if (loadedSongId == null) {
            return -1;
        }

        List<PlaylistEntry> playlistEntries = playlistSongsTableView.getItems();

        for (int index = 0; index < playlistEntries.size(); index++) {
            PlaylistEntry playlistEntry = playlistEntries.get(index);

            int songId = playlistEntry.getSong().getSongId();

            if (loadedSongId.equals(songId)) {
                return index;
            }
        }

        return -1;
    }

    private void loadAndPlayPlaylistEntry(int index) {
        List<PlaylistEntry> playlistEntries = playlistSongsTableView.getItems();

        if (playlistEntries.isEmpty()) {
            showError("Playlist is empty.");
            return;
        }

        if (index < 0 || index >= playlistEntries.size()) {
            return;
        }

        PlaylistEntry playlistEntry = playlistEntries.get(index);
        Song song = playlistEntry.getSong();

        if (!isSongLoaded(song)) {
            loadSong(song);
        }

        mp3Player.play();

        playlistSongsTableView.getSelectionModel().clearAndSelect(index);

        playlistSongsTableView.scrollTo(index);
    }

    private void selectPlaylistSongById(int songId) {
        List<PlaylistEntry> playlistEntries = playlistSongsTableView.getItems();

        for (int index = 0; index < playlistEntries.size(); index++) {
            PlaylistEntry playlistEntry = playlistEntries.get(index);

            int currentSongId = playlistEntry.getSong().getSongId();

            if (currentSongId == songId) {
                playlistSongsTableView.getSelectionModel().clearAndSelect(index);
                playlistSongsTableView.scrollTo(index);
                return;
            }
        }
    }

    private void restoreLastPlaylistSelection() {
        int lastSongId = preferences.getInt(PREFERENCES_LAST_SONG_ID_KEY, PREFERENCES_NO_SONG_ID);

        if (lastSongId == PREFERENCES_NO_SONG_ID) {
            return;
        }

        List<PlaylistEntry> playlistEntries = playlistSongsTableView.getItems();

        for (int index = 0; index < playlistEntries.size(); index++) {
            PlaylistEntry playlistEntry = playlistEntries.get(index);
            Song song = playlistEntry.getSong();

            if (song.getSongId() == lastSongId) {
                playlistSongsTableView.getSelectionModel().clearAndSelect(index);
                playlistSongsTableView.scrollTo(index);

                return;
            }
        }

        preferences.remove(PREFERENCES_LAST_SONG_ID_KEY);
    }

    /*
        TABLE SIZING
     */

    private void resizeColumnToContent(TableColumn<Song, ?> column, Function<Song, String> textGetter) {
        Font tableFont = Font.font("JetBrains Mono", 14);

        Text measuringText = new Text(column.getText());
        measuringText.setFont(tableFont);

        double largestWidth = measuringText.getLayoutBounds().getWidth();

        for (Song song: songsTableView.getItems()) {
            String cellText = textGetter.apply(song);

            if (cellText != null) {
                measuringText.setText(cellText);

                double cellWidth = measuringText.getLayoutBounds().getWidth();

                largestWidth = Math.max(largestWidth, cellWidth);
            }
        }

        column.setPrefWidth(largestWidth + 24);
    }

    private void resizeAllColumnsToContent() {
        songsTableView.setColumnResizePolicy(TableView.UNCONSTRAINED_RESIZE_POLICY);

        resizeColumnToContent(songIdColumn, song -> String.valueOf(song.getSongId()));
        resizeColumnToContent(songArtistColumn, song -> song.getSongArtist());
        resizeColumnToContent(songTitleColumn, song -> song.getSongTitle());
        resizeColumnToContent(songAlbumColumn, song -> song.getSongAlbum());
        resizeColumnToContent(songYearColumn, song -> String.valueOf(song.getSongYear()));
        resizeColumnToContent(songGenreColumn, song -> song.getSongGenre());
        resizeColumnToContent(songFilePathColumn, song -> song.getSongFilePath());
    }

    private void resizePlaylistColumnToContent(TableColumn<PlaylistEntry, ?> column, Function<PlaylistEntry, String> textGetter) {
        Font tableFont = Font.font("JetBrains Mono", 14);

        Text measuringText = new Text(column.getText());
        measuringText.setFont(tableFont);

        double largestWidth = measuringText.getLayoutBounds().getWidth();

        for (PlaylistEntry playlistEntry : playlistSongsTableView.getItems()) {

            String cellText = textGetter.apply(playlistEntry);

            if (cellText != null) {
                measuringText.setText(cellText);

                double cellWidth = measuringText.getLayoutBounds().getWidth();

                largestWidth = Math.max(largestWidth, cellWidth);
            }
        }

        column.setPrefWidth(largestWidth + 24);
    }

    private void resizePlaylistColumnsToContent() {
        playlistSongsTableView.setColumnResizePolicy(TableView.UNCONSTRAINED_RESIZE_POLICY);

        resizePlaylistColumnToContent(playlistPositionColumn, playlistEntry -> String.valueOf(playlistEntry.getPlaylistPosition()));

        resizePlaylistColumnToContent(playlistSongArtistColumn, playlistEntry -> playlistEntry.getSongArtist());

        resizePlaylistColumnToContent(playlistSongTitleColumn, playlistEntry -> playlistEntry.getSongTitle());
    }

    /*
        THEME MANAGEMENT
     */

    private void handleLightThemeButton() {
        preferences.putInt(PREFERENCES_THEME_SETTING_KEY, PREFERENCES_LIGHT_THEME);
        applyTheme(preferences);

        lightThemeButton.setOpacity(ENABLED_BUTTON_OPACITY);
        darkThemeButton.setOpacity(LOWERED_BUTTON_OPACITY);
    }

    private void handleDarkThemeButton() {
        preferences.putInt(PREFERENCES_THEME_SETTING_KEY, PREFERENCES_DARK_THEME);
        applyTheme(preferences);

        lightThemeButton.setOpacity(LOWERED_BUTTON_OPACITY);
        darkThemeButton.setOpacity(ENABLED_BUTTON_OPACITY);
    }

    private void applyTheme(Preferences preferences) {
        if (rootLayout.getScene() == null) {
            Platform.runLater(() -> applyTheme(preferences));
            return;
        }

        rootLayout.getScene().getStylesheets().clear();

        if (preferences.getInt(PREFERENCES_THEME_SETTING_KEY, PREFERENCES_DARK_THEME) == PREFERENCES_DARK_THEME) {
            rootLayout.getScene().getStylesheets().add(
                    Objects.requireNonNull(
                            getClass().getResource(PREFERENCES_DARK_THEME_CSS)).toExternalForm()
            );
        } else {
            rootLayout.getScene().getStylesheets().add(
                    Objects.requireNonNull(
                            getClass().getResource(PREFERENCES_LIGHT_THEME_CSS)).toExternalForm()
            );
        }
    }

    /*
        DIALOGS
     */

    private void showError(String message) {
        Alert alert = new Alert(Alert.AlertType.ERROR);

        alert.setTitle("ERROR");
        alert.setHeaderText(null);
        alert.setContentText(message);

        centerAlertOnMainWindow(alert);

        alert.showAndWait();
    }

    private void centerAlertOnMainWindow(Alert alert) {
        Window ownerWindow = rootLayout.getScene().getWindow();

        alert.initOwner(ownerWindow);

        alert.setOnShown(event -> {
            Window alertWindow = alert.getDialogPane().getScene().getWindow();

            double centerX = ownerWindow.getX() + ownerWindow.getWidth() / 2;

            double centerY = ownerWindow.getY() + ownerWindow.getHeight() / 2;

            alertWindow.setX(centerX - alert.getWidth() / 2);

            alertWindow.setY(centerY - alert.getHeight() / 2);
        });
    }
}
