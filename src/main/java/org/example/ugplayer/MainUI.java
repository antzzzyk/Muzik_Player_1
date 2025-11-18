package org.example.ugplayer;

import javafx.animation.Interpolator;
import javafx.animation.RotateTransition;
import javafx.application.Application;
import javafx.collections.MapChangeListener;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.media.Media;
import javafx.scene.media.MediaPlayer;
import javafx.scene.paint.Color;
import javafx.scene.paint.ImagePattern;
import javafx.scene.shape.Circle;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import javafx.util.Duration;

import java.io.File;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public class MainUI extends Application {

    public static MainUI instance;
    private final List<URL> songURLs = new ArrayList<>();
    private final List<String> songTitles = new ArrayList<>();
    private final List<String> songArtists = new ArrayList<>();
    Label vol = new Label("🔊");
    private Circle albumCover;
    private Circle centerHoleGray;
    private Circle centerHoleWhite;
    private ListView<String> songListView;
    private Label songTitleLabel;
    private Label artistLabel;
    private Button playButton, pauseButton, nextButton, prevButton;
    private Slider volumeSlider;
    private RotateTransition rotateTransition;
    private MediaPlayer mediaPlayer;
    private int currentSongIndex = 0;
    private Scene scene;
    private MiniPlayerUI miniPlayer;

    public static void main(String[] args) {
        launch(args);
    }

    @Override
    public void start(Stage primaryStage) {
        instance = this;

        // === CD COVER ===
        albumCover = new Circle(130, Color.web("#c9c9ca"));
        albumCover.setStroke(Color.DARKGRAY);
        albumCover.setStrokeWidth(3);
        albumCover.setStyle("-fx-effect: dropshadow(gaussian, rgba(0,0,0,0.3), 15, 0.6, 0, 3);");

        centerHoleGray = new Circle(30, Color.web("#b9b9b9"));
        centerHoleWhite = new Circle(23, Color.WHITE);

        StackPane cdPane = new StackPane(albumCover, centerHoleGray, centerHoleWhite);
        cdPane.setAlignment(Pos.CENTER);
        cdPane.setPrefWidth(300);

        // === SONG LIST ===
        songListView = new ListView<>();
        loadSongsFromResources();
        setupSongListContextMenu();

        // === LABELS ===
        songTitleLabel = new Label("Song Title");
        songTitleLabel.setStyle("-fx-font-size: 14px; -fx-font-weight: bold;");
        songTitleLabel.setAlignment(Pos.CENTER);

        artistLabel = new Label("Artist");
        artistLabel.setStyle("-fx-font-size: 13px;");
        artistLabel.setAlignment(Pos.CENTER);

        // === BUTTONS ===
        prevButton = createStyledButton("⏮ Previous", "#9C27B0");
        playButton = createStyledButton("▶ Play", "#4CAF50");
        pauseButton = createStyledButton("⏸ Pause", "#FF9800");
        nextButton = createStyledButton("Next ⏭", "#2196F3");

        // === SLIDER ===
        volumeSlider = new Slider(0, 1, 0.5);

        volumeSlider.setPrefWidth(150);
        volumeSlider.valueProperty().addListener((obs, oldVal, newVal) -> {
            if (mediaPlayer != null) mediaPlayer.setVolume(newVal.doubleValue());
        });

        HBox controls = new HBox(10, prevButton, playButton, pauseButton, nextButton, vol, volumeSlider);
        controls.setAlignment(Pos.CENTER);

        VBox rightPanel = new VBox(10, songListView, songTitleLabel, artistLabel, controls);
        rightPanel.setAlignment(Pos.CENTER);

        // === MENU BAR ===
        MenuBar menuBar = createMenuBar();

        // === MAIN LAYOUT ===
        HBox content = new HBox(30, cdPane, rightPanel);
        content.setAlignment(Pos.CENTER_LEFT);
        content.setPadding(new Insets(30));

        BorderPane root = new BorderPane();
        root.setTop(menuBar);
        root.setCenter(content);

        scene = new Scene(root, 850, 480);
        applyLightTheme();
        primaryStage.setTitle("MUZik");
        primaryStage.setScene(scene);
        primaryStage.show();

        // === EVENTS ===
        songListView.setOnMouseClicked(this::handleDoubleClick);
        playButton.setOnAction(e -> playSong());
        pauseButton.setOnAction(e -> pauseSong());
        nextButton.setOnAction(e -> playNextSong());
        prevButton.setOnAction(e -> playPreviousSong());

        // SPACEBAR PLAY/PAUSE
        scene.setOnKeyPressed(event -> {
            switch (event.getCode()) {
                case SPACE -> {
                    if (mediaPlayer != null) {
                        if (mediaPlayer.getStatus() == MediaPlayer.Status.PLAYING) pauseSong();
                        else playSong();
                    }
                    event.consume();
                }
                default -> {
                }
            }
        });

        // select first song if available
        if (!songURLs.isEmpty()) {
            songListView.getSelectionModel().select(0);
            loadSong(0);
        }

    }

    private void loadSongsFromResources() {
        songURLs.clear();
        songTitles.clear();
        songArtists.clear();
        songListView.getItems().clear();

        List<String> supportedFormats = List.of(".mp3", ".m4a", ".aac", ".wav");

        try {
            URL dirURL = getClass().getResource("/songs");
            if (dirURL != null && dirURL.getProtocol().equals("file")) {
                File folder = new File(dirURL.toURI());
                loadSongsFromFolder(folder, supportedFormats);
            } else {
                File fallbackDir = new File(System.getProperty("user.dir"), "songs");
                if (fallbackDir.exists() && fallbackDir.isDirectory()) {
                    loadSongsFromFolder(fallbackDir, supportedFormats);
                } else {
                    System.out.println("No /songs directory found in resources or fallback ./songs folder.");
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void loadSongsFromFolder(File folder, List<String> supportedFormats) {
        File[] files = folder.listFiles((f, name) -> {
            String lower = name.toLowerCase();
            return supportedFormats.stream().anyMatch(lower::endsWith);
        });

        if (files == null) return;

        for (File file : files) {
            try {
                final URL url = file.toURI().toURL();
                final Media media = new Media(url.toString());
                final String fallbackTitle = file.getName().replaceFirst("\\.[^.]+$", "");
                final String fallbackArtist = "Unknown Artist";

                MediaPlayer tempPlayer = new MediaPlayer(media);
                tempPlayer.setOnReady(() -> {
                    Map<String, Object> meta = media.getMetadata();
                    String title = (String) meta.getOrDefault("title", fallbackTitle);
                    String artist = (String) meta.getOrDefault("artist", fallbackArtist);

                    songURLs.add(url);
                    songTitles.add(title);
                    songArtists.add(artist);
                    songListView.getItems().add(title);

                    tempPlayer.dispose();
                });
            } catch (Exception e) {
                System.err.println("Failed to load: " + file.getName());
                e.printStackTrace();
            }
        }
    }

    private MenuBar createMenuBar() {
        Menu themeMenu = new Menu("Theme");

        //Theme
        MenuItem lightMode = new MenuItem("Light Mode");
        MenuItem darkMode = new MenuItem("Dark Mode");
        MenuItem pinknwhite = new MenuItem("Light Pink");

        //MiniPlayer (Ipod)
        MenuItem imuzikMode = new MenuItem("iMuzik Mode");

        lightMode.setOnAction(e -> applyLightTheme());
        darkMode.setOnAction(e -> applyDarkTheme());
        imuzikMode.setOnAction(e -> openMiniPlayer());
        pinknwhite.setOnAction(e -> applyPinkTheme());


        //Edit
        Menu editMenu = new Menu("Edit");
        MenuItem addSongs = new MenuItem("Add Songs");

        addSongs.setOnAction(e -> addsongs());

        themeMenu.getItems().addAll(lightMode, darkMode, pinknwhite, new SeparatorMenuItem(), imuzikMode);
        editMenu.getItems().addAll(addSongs);
        return new MenuBar(themeMenu, editMenu);
    }

    public void addsongs() {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Add Songs");
        fileChooser.getExtensionFilters().add(
                new FileChooser.ExtensionFilter("Audio Files", "*.mp3", "*.m4a", "*.aac", "*.wav")
        );

        List<File> selectedFiles = fileChooser.showOpenMultipleDialog(scene.getWindow());
        if (selectedFiles == null || selectedFiles.isEmpty()) {
            return;
        }

        try {
            // Try locating the songs directory
            File songsDir;

            URL dirURL = getClass().getResource("/songs");
            if (dirURL != null && dirURL.getProtocol().equals("file")) {
                songsDir = new File(dirURL.toURI());
            } else {
                // fallback to ./songs directory in working folder
                songsDir = new File(System.getProperty("user.dir"), "songs");
            }

            if (!songsDir.exists()) {
                songsDir.mkdirs();
            }

            // Copy files into songs folder
            for (File src : selectedFiles) {
                File dest = new File(songsDir, src.getName());

                // avoid overwriting unless desired
                if (dest.exists()) {
                    System.out.println("File already exists, skipping: " + dest.getName());
                    continue;
                }

                java.nio.file.Files.copy(
                        src.toPath(),
                        dest.toPath(),
                        java.nio.file.StandardCopyOption.REPLACE_EXISTING
                );
                System.out.println("Copied: " + src.getName());
            }

            // Refresh the list to include new songs
            loadSongsFromResources();

            showAlert("Success", "Songs added successfully!");
        } catch (Exception e) {
            e.printStackTrace();
            showAlert("Error", "Failed to add songs: " + e.getMessage());
        }
    }

    private void showAlert(String error, String s) {
    }

    private void openMiniPlayer() {
        if (miniPlayer == null) {
            miniPlayer = new MiniPlayerUI(getMediaPlayer());
        }

        if (miniPlayer != null) {
            miniPlayer.setSharedPlayer(getMediaPlayer());
        }

        miniPlayer.show();
        miniPlayer.update(
                songTitleLabel.getText(),
                artistLabel.getText().replaceFirst("^Artist:\\s*", ""),
                "Unknown Album",
                currentSongIndex,
                Math.max(1, songURLs.size()),
                null
        );
    }

    private void handleDoubleClick(javafx.scene.input.MouseEvent event) {
        if (event.getClickCount() == 2) {
            int index = songListView.getSelectionModel().getSelectedIndex();
            if (index >= 0 && index < songURLs.size()) {
                loadSong(index);
                playSong();
            }
        }
    }

    private void loadSong(int index) {
        stopRotation();
        if (mediaPlayer != null) {
            mediaPlayer.stop();
            mediaPlayer.dispose();
        }

        if (songURLs.isEmpty() || index < 0 || index >= songURLs.size()) return;

        URL songURL = songURLs.get(index);
        Media media = new Media(songURL.toString());
        mediaPlayer = new MediaPlayer(media);
        mediaPlayer.setVolume(volumeSlider.getValue());

        songTitleLabel.setText(songTitles.get(index));
        artistLabel.setText("Artist: " + songArtists.get(index));
        albumCover.setFill(Color.web("#c9c9ca"));

        media.getMetadata().addListener((MapChangeListener<? super String, ? super Object>) change -> {
            if (change.wasAdded()) {
                Map<String, Object> meta = media.getMetadata();
                if (meta.containsKey("title"))
                    songTitleLabel.setText((String) meta.get("title"));
                if (meta.containsKey("artist"))
                    artistLabel.setText("Artist: " + meta.get("artist"));
                if (meta.containsKey("image"))
                    albumCover.setFill(new ImagePattern((Image) meta.get("image")));

                if (miniPlayer != null) {
                    miniPlayer.update(
                            songTitleLabel.getText(),
                            artistLabel.getText().replaceFirst("^Artist:\\s*", ""),
                            "Unknown Album",
                            currentSongIndex,
                            Math.max(1, songURLs.size()),
                            meta.containsKey("image") ? (Image) meta.get("image") : null
                    );
                }
            }
        });

        mediaPlayer.setOnPlaying(this::startRotation);
        mediaPlayer.setOnPaused(this::pauseRotation);
        mediaPlayer.setOnEndOfMedia(this::playNextSong);
        currentSongIndex = index;

        if (miniPlayer != null) {
            miniPlayer.setSharedPlayer(mediaPlayer);
        }
    }

    public void playSong() {
        if (mediaPlayer != null) {
            mediaPlayer.play();
            artistLabel.setText("Now Playing...");
            startRotation();
            if (miniPlayer != null) miniPlayer.syncPlayPause(true);
        }
    }

    public void pauseSong() {
        if (mediaPlayer != null) {
            mediaPlayer.pause();
            artistLabel.setText("Paused");
            pauseRotation();
            if (miniPlayer != null) miniPlayer.syncPlayPause(false);
        }
    }

    public void playNextSong() {
        if (songURLs.isEmpty()) return;
        currentSongIndex = (currentSongIndex + 1) % songURLs.size();
        songListView.getSelectionModel().select(currentSongIndex);
        loadSong(currentSongIndex);
        playSong();
    }

    public void playPreviousSong() {
        if (songURLs.isEmpty()) return;
        currentSongIndex = (currentSongIndex - 1 + songURLs.size()) % songURLs.size();
        songListView.getSelectionModel().select(currentSongIndex);
        loadSong(currentSongIndex);
        playSong();
    }

    private void startRotation() {
        stopRotation(); // ensure only one rotation is active

        rotateTransition = new RotateTransition(Duration.seconds(15), albumCover);
        rotateTransition.setByAngle(360);
        rotateTransition.setCycleCount(RotateTransition.INDEFINITE);
        rotateTransition.setInterpolator(Interpolator.LINEAR);
        rotateTransition.play();
    }

    private void pauseRotation() {
        if (rotateTransition != null) rotateTransition.pause();
    }

    private void stopRotation() {
        if (rotateTransition != null) {
            rotateTransition.stop();
            rotateTransition = null; // important!
        }
        albumCover.setRotate(0);
    }

    private Button createStyledButton(String text, String color) {
        Button btn = new Button(text);
        btn.setStyle("-fx-background-color: " + color + "; -fx-text-fill: white; -fx-font-weight: bold; -fx-background-radius: 8;");
        btn.setOnMouseEntered(e -> btn.setStyle("-fx-background-color: derive(" + color + ", 20%); -fx-text-fill: white; -fx-font-weight: bold; -fx-background-radius: 8;"));
        btn.setOnMouseExited(e -> btn.setStyle("-fx-background-color: " + color + "; -fx-text-fill: white; -fx-font-weight: bold; -fx-background-radius: 8;"));
        return btn;
    }

    private void applyLightTheme() {
        scene.getRoot().setStyle("-fx-background-color: #FAFAFA; -fx-text-fill: black;");
        songListView.setStyle("-fx-control-inner-background: white; -fx-text-fill: black;");
        songTitleLabel.setTextFill(Color.BLACK);
        artistLabel.setTextFill(Color.DARKGRAY);
        centerHoleWhite.setFill(Color.WHITE);
        vol.setStyle("-fx-text-fill: black");
    }

    private void applyDarkTheme() {
        scene.getRoot().setStyle("-fx-background-color: #121212; -fx-text-fill: white;");
        songListView.setStyle("-fx-control-inner-background: #1E1E1E; -fx-text-fill: white;");
        songTitleLabel.setTextFill(Color.WHITE);
        artistLabel.setTextFill(Color.LIGHTGRAY);
        centerHoleWhite.setFill(Color.BLACK);
        vol.setStyle("-fx-text-fill: white");
    }

    private void applyPinkTheme() {
        scene.getRoot().setStyle("-fx-background-color: #f5d7f1;-fx-text-fill: white;");
        songListView.setStyle("-fx-control-inner-background: #f5daed; -fx-text-fill: white;");
        songTitleLabel.setTextFill(Color.WHITE);
        artistLabel.setTextFill(Color.WHITE);
        centerHoleWhite.setFill(Color.rgb(245, 215, 241));
        vol.setStyle("-fx-text-fill: white");
    }

    public MediaPlayer getMediaPlayer() {
        return mediaPlayer;
    }

    private void setupSongListContextMenu() {
        ContextMenu contextMenu = new ContextMenu();

        MenuItem deleteItem = new MenuItem("🗑 Delete from Library");
        deleteItem.setOnAction(e -> deleteSelectedSong());

        contextMenu.getItems().add(deleteItem);

        // attach context menu to list view
        songListView.setContextMenu(contextMenu);
    }

    private void deleteSelectedSong() {
        int selectedIndex = songListView.getSelectionModel().getSelectedIndex();
        if (selectedIndex < 0 || selectedIndex >= songURLs.size()) {
            showAlert("No Selection", "Please select a song to delete.");
            return;
        }

        String songTitle = songTitles.get(selectedIndex);
        URL songURL = songURLs.get(selectedIndex);

        // Confirm deletion
        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
        confirm.setTitle("Delete Song");
        confirm.setHeaderText("Delete \"" + songTitle + "\" from your library?");
        confirm.setContentText("This will permanently remove the file from your songs folder.");
        Optional<ButtonType> result = confirm.showAndWait();

        if (result.isEmpty() || result.get() != ButtonType.OK) return;

        try {
            File songFile = new File(songURL.toURI());

            if (songFile.exists() && songFile.delete()) {
                System.out.println("Deleted: " + songFile.getName());

                // Refresh the entire list to stay consistent
                loadSongsFromResources();

                // Optional: auto-select the next song if available
                if (!songURLs.isEmpty()) {
                    int nextIndex = Math.min(selectedIndex, songURLs.size() - 1);
                    songListView.getSelectionModel().select(nextIndex);
                }

                showAlert("Deleted", "\"" + songFile.getName() + "\" was removed successfully.");
            } else {
                showAlert("Error", "Failed to delete song file. It may be in use.");
            }

        } catch (Exception e) {
            e.printStackTrace();
            showAlert("Error", "Could not delete file: " + e.getMessage());
        }
    }
}
