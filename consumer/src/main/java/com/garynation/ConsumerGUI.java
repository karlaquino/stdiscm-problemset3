package com.garynation;

import javafx.application.Application;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.VBox;
import javafx.scene.media.Media;
import javafx.scene.media.MediaPlayer;
import javafx.scene.media.MediaView;
import javafx.stage.Stage;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.net.ServerSocket;
import java.net.Socket;

public class ConsumerGUI extends Application {
    private int port = 12345; // Change to the desired port
    private String saveDirectory = "uploaded_videos"; // Directory to save videos
    private ListView<String> videoListView;
    private MediaPlayer previewMediaPlayer;
    private MediaView mediaView;

    // Define the size of the preview
    private static final double PREVIEW_WIDTH = 400;
    private static final double PREVIEW_HEIGHT = 300;

    @Override
    public void start(Stage primaryStage) {
        videoListView = new ListView<>();
        mediaView = new MediaView();
        mediaView.setFitWidth(PREVIEW_WIDTH);
        mediaView.setFitHeight(PREVIEW_HEIGHT);
        mediaView.setPreserveRatio(true); // Preserve aspect ratio

        // Create headers for the list and preview area
        Label listHeader = new Label("Uploaded Videos");
        listHeader.setStyle("-fx-font-size: 16px; -fx-font-weight: bold; -fx-padding: 10px;");

        Label previewHeader = new Label("Video Preview");
        previewHeader.setStyle("-fx-font-size: 16px; -fx-font-weight: bold; -fx-padding: 10px;");

        // Set up the layout
        BorderPane borderPane = new BorderPane();
        VBox listVBox = new VBox(listHeader, videoListView);
        listVBox.setPrefWidth(400); // Set the preferred width
        listVBox.setPrefHeight(300); // Set the preferred height
        VBox previewVBox = new VBox(previewHeader, mediaView);

        borderPane.setLeft(listVBox);
        borderPane.setRight(previewVBox);

        Scene scene = new Scene(borderPane, 800, 400);
        primaryStage.setTitle("Video Consumer");
        primaryStage.setScene(scene);
        primaryStage.show();

        // Set the cell factory for the video list
        videoListView.setCellFactory(param -> new VideoListCell());

        videoListView.getSelectionModel().selectedItemProperty().addListener((obs, oldSelection, newSelection) -> {
            if (newSelection != null) {
                playSelectedVideo(newSelection);
            }
        });

        new Thread(this::startServer).start();
    }

    private void startServer() {
        try (ServerSocket serverSocket = new ServerSocket(port)) {
            File dir = new File(saveDirectory);
            if (!dir.exists()) {
                dir.mkdir(); // Create directory if it doesn't exist
            }

            while (true) {
                Socket clientSocket = serverSocket.accept();
                new Thread(new VideoHandler(clientSocket)).start();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private class VideoHandler implements Runnable {
        private Socket clientSocket;

        public VideoHandler(Socket clientSocket) {
            this.clientSocket = clientSocket;
        }

        @Override
        public void run() {
            try (InputStream is = clientSocket.getInputStream()) {
                String videoName = "uploaded_video_" + System.currentTimeMillis() + ".mp4";
                File file = new File(saveDirectory, videoName);
                try (FileOutputStream fos = new FileOutputStream(file)) {
                    byte[] buffer = new byte[4096];
                    int bytesRead;
                    while ((bytesRead = is.read(buffer)) != -1) {
                        fos.write(buffer, 0, bytesRead);
                    }
                }
                // Update the GUI with the new video
                javafx.application.Platform.runLater(() -> videoListView.getItems().add(file.getAbsolutePath()));
                System.out.println("Video uploaded: " + videoName);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    private class VideoListCell extends ListCell<String> {
        public VideoListCell() {
            setOnMouseEntered(event -> {
                String videoPath = getItem();
                if (videoPath != null) {
                    showPreview(videoPath);
                }
            });

            setOnMouseExited(event -> {
                if (previewMediaPlayer != null) {
                    previewMediaPlayer.stop(); // Stop the preview when mouse exits
                }
            });
        }

        @Override
        protected void updateItem(String item, boolean empty) {
            super.updateItem(item, empty);
            if (empty || item == null) {
                setText(null);
                setGraphic(null);
            } else {
                setText(new File(item).getName()); // Display only the file name
            }
        }
    }

    private void showPreview(String videoPath) {
        if (previewMediaPlayer != null) {
            previewMediaPlayer.stop(); // Stop any currently playing video
        }

        Media media = new Media(new File(videoPath).toURI().toString());
        previewMediaPlayer = new MediaPlayer(media);

        previewMediaPlayer.setOnReady(() -> {
            previewMediaPlayer.setStartTime(javafx.util.Duration.seconds(0));
            previewMediaPlayer.setStopTime(javafx.util.Duration.seconds(10));
            previewMediaPlayer.setMute(true);
            previewMediaPlayer.play(); // Play the video for 10 seconds
        });

        mediaView.setMediaPlayer(previewMediaPlayer); // Set the media player to the media view
    }

    private void playSelectedVideo(String videoPath) {
        // Open a new window for the full video playback
        Stage videoStage = new Stage();
        videoStage.setTitle("Video Playback");

        Media media = new Media(new File(videoPath).toURI().toString());
        MediaPlayer playbackMediaPlayer = new MediaPlayer(media);
        MediaView videoMediaView = new MediaView(playbackMediaPlayer);

        Button backButton = new Button("Back to List");
        backButton.setOnAction(e -> {
            videoStage.close(); // Close the video playback window
            if (playbackMediaPlayer != null) {
                playbackMediaPlayer.stop(); // Stop the video if it's playing
            }
        });

        VBox vbox = new VBox(videoMediaView, backButton);
        Scene scene = new Scene(vbox, 600, 400);
        videoStage.setScene(scene);
        videoStage.show();

        playbackMediaPlayer.play(); // Play the full video
    }

    public static void main(String[] args) {
        launch(args);
    }
}
