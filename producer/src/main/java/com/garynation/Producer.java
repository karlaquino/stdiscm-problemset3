package com.garynation;

import java.io.*;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URL;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

public class Producer {
    private final String consumerUrl;
    // private static final int DEFAULT_THREAD_POOL_SIZE = 4;


    public Producer(String consumerUrl) {
        this.consumerUrl = consumerUrl;
    }

    public void uploadVideo(String filePath) {
        File file = new File(filePath);
        if (!file.exists() || !file.isFile()) {
            System.out.println("File not found: " + filePath);
            return;
        }

        try {
            String boundary = "------Boundary" + System.currentTimeMillis();
            HttpURLConnection connection = (HttpURLConnection) URI.create(consumerUrl).toURL().openConnection();

            connection.setDoOutput(true);
            connection.setRequestMethod("POST");
            connection.setRequestProperty("Content-Type", "multipart/form-data; boundary=" + boundary);

            try (OutputStream outputStream = connection.getOutputStream();
                 PrintWriter writer = new PrintWriter(new OutputStreamWriter(outputStream, "UTF-8"), true)) {

                writer.append("--").append(boundary).append("\r\n");
                writer.append("Content-Disposition: form-data; name=\"file\"; filename=\"").append(file.getName()).append("\"\r\n");
                writer.append("Content-Type: ").append(Files.probeContentType(file.toPath())).append("\r\n");
                writer.append("\r\n");
                writer.flush();

                Files.copy(file.toPath(), outputStream);
                outputStream.flush();

                writer.append("\r\n").flush();
                writer.append("--").append(boundary).append("--").append("\r\n");
                writer.flush();
            }

            // Get server response
            int responseCode = connection.getResponseCode();
            if (responseCode == HttpURLConnection.HTTP_OK) {
                System.out.println("Upload successful: " + filePath);
            } else if (responseCode == 429) {
                System.out.println("Queue full, retrying later: " + filePath);
                retryUpload(filePath);
            } else {
                System.out.println("Upload failed. Response code: " + responseCode);
            }

            connection.disconnect();
        } catch (IOException e) {
            System.err.println("Error uploading file: " + filePath);
            e.printStackTrace();
        }
    }

    private void retryUpload(String filePath) {
        try {
            System.out.println("Queue full, retrying in 5 seconds.");
            Thread.sleep(5000); // Wait before retrying
            uploadVideo(filePath);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            System.err.println("Retry interrupted.");
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
            System.out.println("No video files found in: " + directoryPath);
        }
    }

    public void uploadAllVideosFromDirectoriesThreaded(List<String> directoryPaths, int numThreads) {
        ExecutorService executor = Executors.newFixedThreadPool(numThreads);

        directoryPaths.forEach(directoryPath -> executor.submit(() -> uploadAllVideosFromDirectory(directoryPath)));

        executor.shutdown();
        try {
            if (!executor.awaitTermination(600, TimeUnit.SECONDS)) {
                executor.shutdownNow();
            }
        } catch (InterruptedException e) {
            executor.shutdownNow();
        }
        System.out.println("All uploads complete.");
    }

    public static int getThreadPoolSizeFromEnv() {
        String threadPoolSizeStr = System.getenv("PRODUCER_THREAD_POOL_SIZE");
        Scanner scanner = new Scanner(System.in);
        int threadPoolSize;

        try {
            // If environment variable is not set, use default
            if (threadPoolSizeStr == null || threadPoolSizeStr.isEmpty()) {
                // System.out.println("Thread pool size not specified. Using default: " + DEFAULT_THREAD_POOL_SIZE);
                // return DEFAULT_THREAD_POOL_SIZE;

                System.out.println("You haven't set the PRODUCER_THREAD_POOL_SIZE environment variable. Please enter a valid thread pool size (1-20):");
                return promptForValidThreadPoolSize(scanner);
            }
            
            // Check if input is numeric before parsing
            if (!threadPoolSizeStr.matches("\\d+")) {
                System.err.println("Error: Thread pool size must contain only digits. Got: " + threadPoolSizeStr);
                return promptForValidThreadPoolSize(scanner);
            }

            // Try to parse the environment variable
            try {
                threadPoolSize = Integer.parseInt(threadPoolSizeStr);

                // Validate thread pool size is greater than 0
                if (threadPoolSize <= 0) {
                    System.err.println("Error: Thread pool size must be a positive integer and less than 20. Got: " + threadPoolSize);
                    return promptForValidThreadPoolSize(scanner);
                }
                
                // Validate thread pool size is not greater than 20
                if (threadPoolSize > 20) {
                    System.err.println("Error: Thread pool size must not exceed 20. Got: " + threadPoolSize);
                    return promptForValidThreadPoolSize(scanner);
                }

                System.out.println("Using thread pool size: " + threadPoolSize);
                return threadPoolSize;

            } catch (NumberFormatException e) {
                System.err.println("Error: Thread pool size must be a valid integer. Got: " + threadPoolSizeStr);
                return promptForValidThreadPoolSize(scanner);
            }
        } finally {
            // Don't close the scanner here as it would also close System.in
            // We'll close it in the main method instead
        }
    }

    private static int promptForValidThreadPoolSize(Scanner scanner) {
        int threadPoolSize;
        while (true) {
            System.out.println("Please enter a valid thread pool size (1-20):");
            System.out.flush(); // Ensure prompt is displayed
            String input = scanner.nextLine().trim();
            
            if (!input.matches("\\d+")) {
                System.err.println("Input must contain only digits. Please try again.");
                continue;
            }
            
            try {
                threadPoolSize = Integer.parseInt(input);
                
                if (threadPoolSize <= 0) {
                    System.err.println("Thread pool size must be greater than 0. Please try again.");
                    continue;
                }
                
                if (threadPoolSize > 20) {
                    System.err.println("Thread pool size must not exceed 20. Please try again.");
                    continue;
                }
                
                System.out.println("Using thread pool size: " + threadPoolSize);
                return threadPoolSize;
            } catch (NumberFormatException e) {
                System.err.println("Input must be a valid integer. Please try again.");
            }
        }
    }

    public static void main(String[] args) {
        String consumerUrl = "http://localhost:8080/api/videos/upload";
        
        // Ensure output is flushed
        System.out.flush();

        Producer producer = new Producer(consumerUrl);

        int threadPoolSize = getThreadPoolSizeFromEnv();

        List<String> directoryPaths = new ArrayList<>();
        for (int i = 1; i <= threadPoolSize; i++) {
            directoryPaths.add("producer_videos" + i);
        }

        producer.uploadAllVideosFromDirectoriesThreaded(directoryPaths, threadPoolSize);
    }
}
