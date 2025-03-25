package com.garynation.problemset3.consumer_app;

import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Component;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.net.ServerSocket;
import java.net.Socket;

@Component
public class VideoHandler {

    private static final int PORT = 12345;
    private static final String SAVE_DIRECTORY = "uploaded_videos";
    private final ApplicationEventPublisher applicationEventPublisher;

    public VideoHandler(ApplicationEventPublisher applicationEventPublisher) {
        this.applicationEventPublisher = applicationEventPublisher;
    }

    public void startServer() {
        try (ServerSocket serverSocket = new ServerSocket(PORT)) {
            File dir = new File(SAVE_DIRECTORY);
            if (!dir.exists()) {
                dir.mkdir();
            }

            while (true) {
                Socket clientSocket = serverSocket.accept();
                new Thread(new VideoUploadHandler(clientSocket, SAVE_DIRECTORY, applicationEventPublisher)).start();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private static class VideoUploadHandler implements Runnable {
        private final Socket clientSocket;
        private final String saveDirectory;
        private final ApplicationEventPublisher applicationEventPublisher;

        public VideoUploadHandler(Socket clientSocket, String saveDirectory, ApplicationEventPublisher applicationEventPublisher) {
            this.clientSocket = clientSocket;
            this.saveDirectory = saveDirectory;
            this.applicationEventPublisher = applicationEventPublisher;
        }

        @Override
        public void run() {
            try (InputStream is = clientSocket.getInputStream()) {
                String videoName = "uploaded_video_" + System.currentTimeMillis() + ".mp4";
                File file = new File(saveDirectory, videoName);
                try (FileOutputStream fos = new FileOutputStream(file)) {
                    byte[] buffer = new byte[16384];
                    int bytesRead;
                    while ((bytesRead = is.read(buffer)) != -1) {
                        fos.write(buffer, 0, bytesRead);
                    }
                }
                System.out.println("Video uploaded: " + videoName);
                applicationEventPublisher.publishEvent(new NewVideoEvent());
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    public static String getSaveDirectory() {
        return SAVE_DIRECTORY;
    }
}