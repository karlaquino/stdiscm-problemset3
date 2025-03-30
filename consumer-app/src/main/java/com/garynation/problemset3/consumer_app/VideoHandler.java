package com.garynation.problemset3.consumer_app;

import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Component;

import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.channels.FileChannel;
import java.nio.channels.FileLock;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

@Component
public class VideoHandler {

    private static final int PORT = 12345;
    private static final String SAVE_DIRECTORY = "uploaded_videos";

    // Thread pool and queue configuration
    private static final int THREAD_POOL_SIZE = 1; // Number of threads available
    private static final int MAX_QUEUE_SIZE = 2;  // Maximum length of the queue

    private final ApplicationEventPublisher applicationEventPublisher;
//    private final ExecutorService threadPool; // Thread pool for handling uploads
    private final BlockingQueue<Socket> queue; // Queue for managing tasks
    private final AtomicInteger queueSize = new AtomicInteger(0);

    private final ThreadPoolExecutor threadPool;
    private final Semaphore semaphore;

    private final VideoCompressionService compressionService;


    public VideoHandler(ApplicationEventPublisher applicationEventPublisher,
                        VideoCompressionService compressionService
    ) {
        this.applicationEventPublisher = applicationEventPublisher;
        this.compressionService = compressionService;

        this.queue = new ArrayBlockingQueue<>(MAX_QUEUE_SIZE);

        // Create a semaphore with THREAD_POOL_SIZE + MAX_QUEUE_SIZE permits
        this.semaphore = new Semaphore(THREAD_POOL_SIZE + MAX_QUEUE_SIZE);
//        this.threadPool = Executors.newFixedThreadPool(THREAD_POOL_SIZE);
        this.threadPool = new ThreadPoolExecutor(
                THREAD_POOL_SIZE,        // Core thread pool size
                THREAD_POOL_SIZE,        // Maximum thread pool size (fixed)
                0L, TimeUnit.MILLISECONDS, // Keep-alive time (not used for fixed size)
                new ArrayBlockingQueue<>(MAX_QUEUE_SIZE), // Queue for excess tasks
                new ThreadPoolExecutor.AbortPolicy()
        );


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
                try {
                    // If the queue is full, send a message to the client
                    if (semaphore.availablePermits() == 0) {
                        try (PrintWriter writer = new PrintWriter(clientSocket.getOutputStream(), true)) {
                            writer.println("QUEUE_FULL");
                            System.out.println("Sent QUEUE_FULL to client.");
                        }
                    }

                    // Wait until there's space in the queue
                    semaphore.acquire();

                    System.out.println("Space available, processing request.");

                    threadPool.execute(() -> {
                        try {
                            try (PrintWriter writer = new PrintWriter(clientSocket.getOutputStream(), true)) {
                                writer.println("QUEUE_READY");
                                System.out.println("Sent QUEUE_READY to client.");
                            } catch (IOException e) {
                                System.out.println("Failed to send QUEUE_READY signal: " + e.getMessage());
                            }
                            handleUpload(clientSocket);
                        } finally {
                            semaphore.release(); // Release the semaphore after processing
                        }
                    });

                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    System.out.println("Thread interrupted while waiting for queue space.");
                    try {
                        clientSocket.close();
                    } catch (IOException ex) {
                        ex.printStackTrace();
                    }
                }

                System.out.println("Task submitted. Acquired Sempahores: " + semaphore.availablePermits());

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
                File originalFile = new File(SAVE_DIRECTORY, finalFileName);

                // Writing the file with locking in a completely separate try block
                try (FileOutputStream fos = new FileOutputStream(originalFile);
                     FileChannel fileChannel = fos.getChannel()) {

                    FileLock lock = null;
                    try {
                        lock = fileChannel.tryLock();
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
                    } finally {
                        if (lock != null) {
                            lock.release();
                        }
                    }
                }
                // All resources from file operations are now closed

                // Compress the video
                String compressedFileName = compressionService.compressVideo(originalFile.getAbsolutePath());

                // Replace the original file with the compressed file
                if (compressedFileName != null) {
                    File compressedFile = new File(compressedFileName);
                    if (compressedFile.exists()) {
                        Files.move(Paths.get(compressedFile.getAbsolutePath()), Paths.get(originalFile.getAbsolutePath()), StandardCopyOption.REPLACE_EXISTING);
                        System.out.println("File compressed and replaced: " + finalFileName);
                    } else {
                        System.out.println("Compressed file not found.");
                    }

                } else {
                    System.out.println("Compression failed.");
                }
            }

            // Send success message back immediately after saving
            try (PrintWriter writer = new PrintWriter(new OutputStreamWriter(outputStream), true)) {
                writer.println("UPLOAD_SUCCESS:" + finalFileName);
            }


        } catch (Exception e) {
            System.out.println("\nError: " + e.getMessage() + " - " + clientSocket.getRemoteSocketAddress());
            try (PrintWriter writer = new PrintWriter(clientSocket.getOutputStream())) {
                writer.println("UPLOAD_FAILED");
                writer.flush();
            } catch (IOException ignored) {}
        } finally {
            try {
                clientSocket.close();
            } catch (IOException e) {
                e.printStackTrace();
            }

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