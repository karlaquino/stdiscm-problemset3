package com.garynation;

import javafx.application.Application;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.layout.VBox;
import javafx.scene.control.Label;

import java.io.File;

public class ProducerGUI extends Application {
    private String consumerHost = "localhost"; // Change to consumer's IP if needed
    private int consumerPort = 12345; // Change to the consumer's port

    @Override
    public void start(Stage primaryStage) {
        primaryStage.setTitle("Video Producer");

        Label label = new Label("Select a video file to upload:");
        Button uploadButton = new Button("Upload Video");

        uploadButton.setOnAction(e -> uploadVideo(primaryStage));

        VBox vbox = new VBox(label, uploadButton);
        Scene scene = new Scene(vbox, 300, 200);
        primaryStage.setScene(scene);
        primaryStage.show();
    }

    private void uploadVideo(Stage stage) {
        FileChooser fileChooser = new FileChooser();
        fileChooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("Video Files", "*.mp4", "*.avi", "*.mov"));
        File selectedFile = fileChooser.showOpenDialog(stage);

        if (selectedFile != null) {
            new Thread(() -> {
                Producer producer = new Producer(consumerHost, consumerPort);
                producer.uploadVideo(selectedFile.getAbsolutePath());
            }).start();
        }
    }

    public static void main(String[] args) {
        launch(args);
    }
}
