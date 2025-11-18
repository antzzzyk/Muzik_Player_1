package org.example.ugplayer;

import javafx.application.Platform;
import javafx.beans.value.ChangeListener;
import javafx.collections.MapChangeListener;
import javafx.collections.ObservableMap;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressBar;
import javafx.scene.image.Image;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.media.Media;
import javafx.scene.media.MediaPlayer;
import javafx.scene.paint.Color;
import javafx.scene.paint.ImagePattern;
import javafx.scene.shape.Circle;
import javafx.scene.shape.Rectangle;
import javafx.scene.text.Font;
import javafx.scene.text.Text;
import javafx.stage.Stage;
import javafx.stage.StageStyle;
import javafx.util.Duration;

import java.util.Map;

public class MiniPlayerUI {

    private final Stage stage;
    private final Label titleLabel;
    private final Label artistLabel;
    private final Label albumLabel;
    private final Label positionLabel;
    private final Label currentTimeLabel;
    private final Label totalTimeLabel;
    private final Rectangle albumCover;
    private final Button playPauseBtn;
    private final ProgressBar progressBar;

    private boolean isPlaying = false;
    private MediaPlayer sharedPlayer;

    public MiniPlayerUI(MediaPlayer sharedPlayer) {
        stage = new Stage(StageStyle.TRANSPARENT);
        stage.setAlwaysOnTop(true);

        VBox frame = new VBox();
        frame.setStyle("-fx-background-color: #8abfe8; -fx-border-color: #333; -fx-border-width: 2; "
                + "-fx-border-radius: 12; -fx-background-radius: 12;");
        frame.setPadding(new Insets(8));
        frame.setSpacing(8);
        frame.setAlignment(Pos.TOP_CENTER);

        VBox screen = new VBox(6);
        screen.setAlignment(Pos.CENTER_LEFT);
        screen.setPadding(new Insets(6));
        screen.setStyle("-fx-background-color: #EEEEEE; -fx-border-color: black; -fx-border-width: 2; "
                + "-fx-border-radius: 6; -fx-background-radius: 6;");

        albumCover = new Rectangle(70, 70, Color.GRAY);

        VBox textBox = new VBox(2);

        // === Title Label ===
        titleLabel = new Label("Song Title");
        titleLabel.setStyle("-fx-font-weight: bold;");
        titleLabel.setAlignment(Pos.CENTER_LEFT);
        titleLabel.setWrapText(true);
        titleLabel.setPrefWidth(120);
        titleLabel.setMaxWidth(120);
        titleLabel.setMinHeight(36);

        // === Artist Label ===
        artistLabel = new Label("Artist");
        artistLabel.setAlignment(Pos.CENTER_LEFT);
        artistLabel.setWrapText(false);
        artistLabel.setPrefWidth(120);
        artistLabel.setMaxWidth(120);
        artistLabel.setMinHeight(16);

        // Apply dynamic font resizing
        setupAutoFontSize(titleLabel, 13, 9);
        setupAutoFontSize(artistLabel, 12, 8);

        albumLabel = new Label("Album");
        positionLabel = new Label("1 of 3");

        textBox.getChildren().addAll(titleLabel, artistLabel, albumLabel, positionLabel);

        HBox screenContent = new HBox(8, albumCover, textBox);
        screenContent.setAlignment(Pos.CENTER_LEFT);
        screen.getChildren().add(screenContent);

        // === Progress Bar + Time Labels ===
        progressBar = new ProgressBar(0);
        progressBar.setPrefWidth(150);
        progressBar.setStyle("-fx-accent: #007BFF;"); // blue fill

        currentTimeLabel = new Label("0:00");
        currentTimeLabel.setStyle("-fx-font-size: 10px; -fx-text-fill: #555;");
        totalTimeLabel = new Label("0:00");
        totalTimeLabel.setStyle("-fx-font-size: 10px; -fx-text-fill: #555;");

        HBox progressRow = new HBox(6, currentTimeLabel, progressBar, totalTimeLabel);
        progressRow.setAlignment(Pos.CENTER);
        HBox.setHgrow(progressBar, Priority.ALWAYS);

        // === Click-to-seek feature ===
        progressBar.setOnMouseClicked(e -> {
            if (sharedPlayer != null && sharedPlayer.getTotalDuration() != null
                    && !sharedPlayer.getTotalDuration().isUnknown()) {
                double clickX = e.getX();
                double width = progressBar.getWidth();
                double ratio = Math.min(Math.max(clickX / width, 0), 1);
                sharedPlayer.seek(sharedPlayer.getTotalDuration().multiply(ratio));
            }
        });

        screen.getChildren().add(progressRow);

        // === Click Wheel ===
        Circle outerCircle = new Circle(55, Color.web("#F1F6F6"));
        outerCircle.setStroke(Color.BLACK);
        outerCircle.setStrokeWidth(0.3);

        Circle innerCircle = new Circle(23, Color.web("#8abfe8"));
        innerCircle.setStroke(Color.BLACK);
        innerCircle.setStrokeWidth(0.3);

        Label menuLabel = new Label("MENU");
        menuLabel.setStyle("-fx-font-weight: bold; -fx-font-size: 12px; -fx-text-fill: #bdbcbc;");
        StackPane.setAlignment(menuLabel, Pos.TOP_CENTER);
        StackPane.setMargin(menuLabel, new Insets(8, 0, 0, 0));

        Button prevBtn = createWheelButton("⏮");
        Button nextBtn = createWheelButton("⏭");
        playPauseBtn = createWheelButton("▶⏸");

        StackPane.setAlignment(prevBtn, Pos.CENTER_LEFT);
        StackPane.setMargin(prevBtn, new Insets(0, 0, 0, 50));
        StackPane.setAlignment(nextBtn, Pos.CENTER_RIGHT);
        StackPane.setMargin(nextBtn, new Insets(0, 50, 0, 0));
        StackPane.setAlignment(playPauseBtn, Pos.BOTTOM_CENTER);
        StackPane.setMargin(playPauseBtn, new Insets(0, 0, 8, 0));

        StackPane wheel = new StackPane(outerCircle, innerCircle, menuLabel, prevBtn, nextBtn, playPauseBtn);
        wheel.setPrefSize(140, 140);
        wheel.setAlignment(Pos.CENTER);

        frame.getChildren().addAll(screen, wheel);

        // === Scene ===
        Scene scene = new Scene(frame, 240, 280, Color.TRANSPARENT);
        stage.setScene(scene);

        // === Buttons ===
        prevBtn.setOnAction(e -> {
            if (MainUI.instance != null) MainUI.instance.playPreviousSong();
        });
        nextBtn.setOnAction(e -> {
            if (MainUI.instance != null) MainUI.instance.playNextSong();
        });
        playPauseBtn.setOnAction(e -> togglePlayPause());

        // === Drag Window ===
        final double[] offset = new double[2];
        frame.setOnMousePressed(e -> {
            offset[0] = e.getSceneX();
            offset[1] = e.getSceneY();
        });
        frame.setOnMouseDragged(e -> {
            stage.setX(e.getScreenX() - offset[0]);
            stage.setY(e.getScreenY() - offset[1]);
        });

        if (sharedPlayer != null) setSharedPlayer(sharedPlayer);
    }

    public void show() {
        stage.show();
    }

    /**
     * Updates text and image manually (still supported)
     */
    public void update(String title, String artist, String album, int index, int total, Image artImage) {
        titleLabel.setText(title != null ? title : "Unknown Title");
        artistLabel.setText(artist != null ? artist : "Unknown Artist");
        albumLabel.setText(album != null ? album : "Unknown Album");
        positionLabel.setText((index + 1) + " of " + total);

        if (artImage != null)
            albumCover.setFill(new ImagePattern(artImage));
        else
            albumCover.setFill(Color.GRAY);
    }

    public void setSharedPlayer(MediaPlayer player) {
        this.sharedPlayer = player;
        if (player == null) return;

        Media media = player.getMedia();
        if (media != null) loadMetadata(media);

        player.statusProperty().addListener((obs, old, now) -> syncPlayPause(now == MediaPlayer.Status.PLAYING));
        player.currentTimeProperty().addListener((obs, oldTime, newTime) -> {
            if (player.getTotalDuration() != null && !player.getTotalDuration().isUnknown()) {
                double progress = newTime.toSeconds() / player.getTotalDuration().toSeconds();
                progressBar.setProgress(progress);
                currentTimeLabel.setText(formatTime(newTime));
                totalTimeLabel.setText(formatTime(player.getTotalDuration()));
            }
        });
        player.setOnReady(() -> {
            Duration total = player.getMedia().getDuration();
            progressBar.setProgress(0);
            currentTimeLabel.setText("0:00");
            totalTimeLabel.setText(formatTime(total));
        });
    }

    /**
     * Reads and updates metadata from the MP3 file
     */
    private void loadMetadata(Media media) {
        if (media == null) return;

        ObservableMap<String, Object> metadata = media.getMetadata();

        // Listener for metadata changes
        metadata.addListener((MapChangeListener<String, Object>) change -> Platform.runLater(() -> {
            if (change.wasAdded()) {
                String key = change.getKey();
                Object val = change.getValueAdded();

                if (val == null) return;

                switch (key.toLowerCase()) {
                    case "title" -> titleLabel.setText(val.toString());
                    case "artist" -> artistLabel.setText(val.toString());
                    case "album" -> albumLabel.setText(val.toString());
                    case "image" -> {
                        if (val instanceof Image image) albumCover.setFill(new ImagePattern(image));
                    }
                }
            }
        }));

        // Read initial metadata that might already be present
        for (Map.Entry<String, Object> entry : metadata.entrySet()) {
            String key = entry.getKey();
            Object val = entry.getValue();
            if (val == null) continue;

            switch (key.toLowerCase()) {
                case "title" -> titleLabel.setText(val.toString());
                case "artist" -> artistLabel.setText(val.toString());
                case "album" -> albumLabel.setText(val.toString());
                case "image" -> {
                    if (val instanceof Image image) albumCover.setFill(new ImagePattern(image));
                }
            }
        }
    }

    private void togglePlayPause() {
        if (MainUI.instance == null || MainUI.instance.getMediaPlayer() == null) return;
        MediaPlayer player = MainUI.instance.getMediaPlayer();

        if (player.getStatus() == MediaPlayer.Status.PLAYING) {
            MainUI.instance.pauseSong();
            syncPlayPause(false);
        } else {
            MainUI.instance.playSong();
            syncPlayPause(true);
        }
    }

    void syncPlayPause(boolean playing) {
        isPlaying = playing;
    }

    private String formatTime(Duration time) {
        int seconds = (int) time.toSeconds();
        int minutes = seconds / 60;
        int sec = seconds % 60;
        return String.format("%d:%02d", minutes, sec);
    }

    private Button createWheelButton(String symbol) {
        Button btn = new Button(symbol);
        btn.setStyle("-fx-background-color: transparent; -fx-font-size: 14px; -fx-font-weight: bold; -fx-text-fill: #bdbcbc;");
        return btn;
    }

    // Automatically resize font to fit width (robust version)
    private void setupAutoFontSize(Label label, double maxFont, double minFont) {
        ChangeListener<Object> resizeListener = (obs, oldVal, newVal) -> {
            Platform.runLater(() -> {
                double labelWidth = label.getWidth();
                if (labelWidth <= 0) labelWidth = label.getPrefWidth();

                if (labelWidth <= 0 || label.getText() == null) return;

                Text text = new Text(label.getText());
                double fontSize = maxFont;
                text.setFont(Font.font(fontSize));

                while (text.getLayoutBounds().getWidth() > labelWidth && fontSize > minFont && !label.isWrapText()) {
                    fontSize -= 0.3;
                    text.setFont(Font.font(fontSize));
                }
                label.setFont(Font.font(fontSize));
            });
        };

        label.widthProperty().addListener(resizeListener);
        label.textProperty().addListener(resizeListener);

        label.sceneProperty().addListener((obs, oldScene, newScene) -> {
            if (newScene != null) {
                newScene.windowProperty().addListener((o2, oldWin, newWin) -> {
                    if (newWin != null) {
                        newWin.showingProperty().addListener((o3, wasShowing, isShowing) -> {
                            if (isShowing) Platform.runLater(() -> resizeListener.changed(null, null, null));
                        });
                    }
                });
            }
        });
    }
}
