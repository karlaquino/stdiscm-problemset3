package com.garynation;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.net.Socket;

public class Producer {
    private String consumerHost;
    private int consumerPort;

    public Producer(String consumerHost, int consumerPort) {
        this.consumerHost = consumerHost;
        this.consumerPort = consumerPort;
    }

    public void uploadVideo(String filePath) {
        File file = new File(filePath);
        try (Socket socket = new Socket(consumerHost, consumerPort);
             FileInputStream fis = new FileInputStream(file);
             OutputStream os = socket.getOutputStream()) {

            byte[] buffer = new byte[4096];
            int bytesRead;
            while ((bytesRead = fis.read(buffer)) != -1) {
                os.write(buffer, 0, bytesRead);
            }
            os.flush();
            System.out.println("Upload complete: " + file.getName());
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void uploadAllVideosFromDirectory(String directoryPath) {
        File dir = new File(directoryPath);
        File[] files = dir.listFiles((d, name) -> name.endsWith(".mp4") || name.endsWith(".avi") || name.endsWith(".mov"));

        if (files != null) {
            for (File file : files) {
                uploadVideo(file.getAbsolutePath());
            }
        } else {
            System.out.println("No video files found in the specified directory.");
        }
    }

    public static void main(String[] args) {
        String consumerHost = "localhost"; // Change to consumer's IP if needed
        int consumerPort = 12345; // Change to the consumer's port
        String directoryPath = "producer_videos"; // Specify your directory path here

        Producer producer = new Producer(consumerHost, consumerPort);
        producer.uploadAllVideosFromDirectory(directoryPath);
    }
}
