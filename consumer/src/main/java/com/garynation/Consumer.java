package com.garynation;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.net.ServerSocket;
import java.net.Socket;

public class Consumer {
    private int port;
    private String saveDirectory;
    private static final int MAX_QUEUE_LENGTH = 10; // Example value

    public Consumer(int port, String saveDirectory) {
        this.port = port;
        this.saveDirectory = saveDirectory;
    }

    public void start() {
        try (ServerSocket serverSocket = new ServerSocket(port)) {
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
                File file = new File(saveDirectory, "uploaded_video.mp4");
                try (FileOutputStream fos = new FileOutputStream(file)) {
                    byte[] buffer = new byte[4096];
                    int bytesRead;
                    while ((bytesRead = is.read(buffer)) != -1) {
                        fos.write(buffer, 0, bytesRead);
                    }
                }
                // Here you can add code to handle video preview and GUI updates
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }
}
