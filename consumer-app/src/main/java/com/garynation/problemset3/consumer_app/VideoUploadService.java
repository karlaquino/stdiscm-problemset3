package com.garynation.problemset3.consumer_app;

import com.garynation.problemset3.consumer_app.config.VideoUploadProperties;
import org.springframework.stereotype.Service;

import java.io.*;
import java.nio.channels.FileChannel;
import java.nio.channels.FileLock;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.concurrent.*;
import java.util.logging.Logger;

@Service
public class VideoUploadService {

    private static final String SAVE_DIRECTORY = "uploaded_videos";
    private static final Logger logger = Logger.getLogger(VideoUploadService.class.getName());

    private final ThreadPoolExecutor threadPool;
    private final BlockingQueue<Runnable> workQueue;
    private final VideoCompressionService compressionService;

    /**
     * Creates a video upload service with custom thread pool and queue size
     *
     * @param compressionService Service for compressing videos
     * @param properties Configuration properties
     */
    public VideoUploadService(VideoCompressionService compressionService, VideoUploadProperties properties) {
        // Create directory if it doesn't exist
        createSaveDirectory();

        // Initialize the queue with specified size
        this.workQueue = new LinkedBlockingQueue<>(properties.getQueueSize());

        // Initialize thread pool with custom rejection policy
        this.threadPool = new ThreadPoolExecutor(
                properties.getThreadPoolSize(),
                properties.getThreadPoolSize(),
                0L,
                TimeUnit.MILLISECONDS,
                workQueue,
                (r, executor) -> {
                    logger.warning("Task rejected: Queue is full");
                    throw new RejectedExecutionException("Video upload queue is full, try again later");
                }
        );

        this.compressionService = compressionService;
    }

    /**
     * Ensures the upload directory exists
     */
    private void createSaveDirectory() {
        File directory = new File(SAVE_DIRECTORY);
        if (!directory.exists()) {
            directory.mkdirs();
        }
    }

    /**
     * Uploads a video asynchronously, compresses it, and handles any filename conflicts
     *
     * @param videoData Input stream containing the video data
     * @param fileName Original filename of the video
     * @return CompletableFuture that completes when the upload is finished
     */
    public CompletableFuture<String> uploadVideo(InputStream videoData, String fileName) {
        CompletableFuture<String> future = new CompletableFuture<>();

        threadPool.execute(() -> {
            try {

                // Synchronize on the file-specific lock
                synchronized (this) {
                    String uniqueFileName = resolveFileNameConflict(fileName);
                    File originalFile = new File(SAVE_DIRECTORY, uniqueFileName);

                    // Save the original file to a temporary location using NIO with file locking
                    try (
                            FileOutputStream outputStream = new FileOutputStream(originalFile);
                            FileChannel channel = outputStream.getChannel();

                    ) {
                        FileLock lock = null;
                        try{
                            lock = channel.tryLock();
                            if (lock == null) {
                                throw new IOException("Failed to acquire file lock.");
                            }
                            // Write to the locked file
                            byte[] buffer = new byte[8192];
                            int bytesRead;
                            while ((bytesRead = videoData.read(buffer)) != -1) {
                                outputStream.write(buffer, 0, bytesRead);
                            }
                        }  finally {
                            if (lock != null) {lock.release();}

                        }


                    }
                    // Compress the video - here we also need file locking
                    String compressedFilePath = compressionService.compressVideo(originalFile.getAbsolutePath());

                    // If compression was successful, move compressed file to final location
                    File compressedFile = new File(compressedFilePath);
                    if (compressedFile.exists()) {
                        Files.move(Paths.get(compressedFile.getAbsolutePath()), Paths.get(originalFile.getAbsolutePath()), StandardCopyOption.REPLACE_EXISTING);
                        System.out.println("File compressed and replaced: " + uniqueFileName);
                    } else {
                        System.out.println("Compressed file not found.");
                    }

                    logger.info("Video uploaded successfully: " + uniqueFileName);
                    future.complete(uniqueFileName);
                }
            } catch (Exception e) {
                logger.severe("Error processing video: " + e.getMessage());
                future.completeExceptionally(e);
            }
        });

        return future;
    }

    /**
     * Ensures unique file names to prevent overwrites.
     * This method is already synchronized to handle concurrent access.
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

    /**
     * Gracefully shuts down the thread pool
     */
    public void shutdown() {
        threadPool.shutdown();
        try {
            if (!threadPool.awaitTermination(60, TimeUnit.SECONDS)) {
                threadPool.shutdownNow();
            }
        } catch (InterruptedException e) {
            threadPool.shutdownNow();
            Thread.currentThread().interrupt();
        }
    }


}