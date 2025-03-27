package com.garynation.problemset3.consumer_app;

import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Component;

import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.channels.FileChannel;
import java.nio.channels.FileLock;
import java.util.concurrent.*;

@Component
public class VideoHandler {

    private static final int PORT = 12345;
    private static final String SAVE_DIRECTORY = "uploaded_videos";

    // Thread pool and queue configuration
    private static final int THREAD_POOL_SIZE = 3; // Number of threads available
    private static final int MAX_QUEUE_SIZE = 4;  // Maximum length of the queue

    private final ApplicationEventPublisher applicationEventPublisher;
    private final ExecutorService threadPool; // Thread pool for handling uploads
    private final BlockingQueue<Socket> queue; // Queue for managing tasks

    public VideoHandler(ApplicationEventPublisher applicationEventPublisher) {
        this.applicationEventPublisher = applicationEventPublisher;

        this.queue = new ArrayBlockingQueue<>(MAX_QUEUE_SIZE);

        this.threadPool = Executors.newFixedThreadPool(THREAD_POOL_SIZE);
    }

    public void startServer() {
        try (ServerSocket serverSocket = new ServerSocket(PORT)) {
            // Create directory for uploaded videos if it doesn't exist
            File dir = new File(SAVE_DIRECTORY);
            if (!dir.exists()) {
                dir.mkdir();
            }

            System.out.println("Server started on port " + PORT);
            while (true) {
                Socket clientSocket = serverSocket.accept();

                // Add the socket to the queue for proper queuing
                if (!queue.offer(clientSocket)) {
                    System.out.println("Queue full. Rejecting video upload.");
                    try (OutputStream os = clientSocket.getOutputStream();
                         PrintWriter writer = new PrintWriter(os)) {
                        writer.println("QUEUE_FULL");
                        writer.flush();
                    } catch (IOException ignored) {}
                    clientSocket.close();
                } else {
                    threadPool.execute(() -> handleUpload(clientSocket));
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            threadPool.shutdown();
        }
    }

    private void handleUpload(Socket clientSocket) {
        try (InputStream inputStream = clientSocket.getInputStream();
             DataInputStream dataInputStream = new DataInputStream(inputStream);
             OutputStream outputStream = clientSocket.getOutputStream()) {

            // Read the filename from the client
            String originalFileName = dataInputStream.readUTF();
            String finalFileName;

            // Synchronize file name resolution to prevent overwrites
            synchronized (this) {
                finalFileName = resolveFileNameConflict(originalFileName);
                File finalFile = new File(SAVE_DIRECTORY, finalFileName);

                // Writing the file with locking
                try (FileOutputStream fos = new FileOutputStream(finalFile);
                     FileChannel fileChannel = fos.getChannel();
                     FileLock lock = fileChannel.tryLock()) {

                    if (lock == null) {
                        throw new IOException("Failed to acquire file lock.");
                    }

                    byte[] buffer = new byte[4096];
                    int bytesRead;
                    while ((bytesRead = dataInputStream.read(buffer)) != -1) {
                        fos.write(buffer, 0, bytesRead);
                    }
                    fos.flush();

                    System.out.println("File saved: " + finalFileName);
                }
            }

            // Send success message back
            PrintWriter writer = new PrintWriter(outputStream);
            writer.println("UPLOAD_SUCCESS:" + finalFileName);
            writer.flush();

        } catch (Exception e) {
            e.printStackTrace();
            try (PrintWriter writer = new PrintWriter(clientSocket.getOutputStream())) {
                writer.println("UPLOAD_FAILED");
                writer.flush();
            } catch (IOException ignored) {}
        } finally {
            try {
                queue.remove(clientSocket);
                clientSocket.close();
            } catch (IOException ignored) {}
        }
    }

    /**
     * Ensures unique file names to prevent overwrites.
     */
    private synchronized String resolveFileNameConflict(String originalFileName) {
        File file = new File(SAVE_DIRECTORY, originalFileName);
        if (!file.exists()) {
            return originalFileName; // No conflict
        }
        String baseName = originalFileName;
        String extension = "";
        int lastDotPosition = originalFileName.lastIndexOf('.');
        if (lastDotPosition > 0) {
            baseName = originalFileName.substring(0, lastDotPosition);
            extension = originalFileName.substring(lastDotPosition);
        }
        int counter = 1;
        String candidateName;
        do {
            candidateName = baseName + "(" + counter + ")" + extension;
            file = new File(SAVE_DIRECTORY, candidateName);
            counter++;
        } while (file.exists());
        return candidateName;
    }

    public static String getSaveDirectory() {
        return SAVE_DIRECTORY;
    }
}