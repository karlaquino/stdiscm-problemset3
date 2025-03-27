package com.garynation;

import java.io.*;
import java.net.Socket;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class Producer {
    private final String consumerHost;
    private final int consumerPort;

    public Producer(String consumerHost, int consumerPort) {
        this.consumerHost = consumerHost;
        this.consumerPort = consumerPort;
    }

    public void uploadVideo(String filePath) {
        File file = new File(filePath);
        try (Socket socket = new Socket(consumerHost, consumerPort);
             FileInputStream fis = new FileInputStream(file);
             OutputStream os = socket.getOutputStream();
             DataOutputStream dos = new DataOutputStream(os);
             InputStreamReader streamReader = new InputStreamReader(socket.getInputStream());
             BufferedReader reader = new BufferedReader(streamReader)) {

            dos.writeUTF(file.getName());
            dos.flush();

            byte[] buffer = new byte[4096];
            int bytesRead;
            while ((bytesRead = fis.read(buffer)) != -1) {
                dos.write(buffer, 0, bytesRead);
            }
            dos.flush();
            socket.shutdownOutput();

            String response = reader.readLine(); // This waits for the server response
            System.out.println("Server Response: " + response);
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
            System.out.println("No video files found in the specified directory: " + directoryPath);
        }
    }

    public void uploadAllVideosFromDirectoriesThreaded(List<String> directoryPaths, int numThreads) {
        ExecutorService executor = Executors.newFixedThreadPool(numThreads);

            directoryPaths.forEach(directoryPath -> executor.submit(() -> uploadAllVideosFromDirectory(directoryPath)));

            executor.shutdown();
            while (!executor.isTerminated()) {
                // Wait for all threads to finish
            }
        System.out.println("All uploads complete from all directories.");
    }

    public static void main(String[] args) {
        String consumerHost = "localhost";
        int consumerPort = 12345;

        Producer producer = new Producer(consumerHost, consumerPort);

        Scanner scanner = new Scanner(System.in);
        System.out.print("Enter the number of threads: ");
        int numThreads = scanner.nextInt();

        List<String> directoryPaths = new ArrayList<>();
        for (int i = 1; i <= numThreads; i++) {
            directoryPaths.add("producer_videos" + i);
        }

        scanner.close();

        producer.uploadAllVideosFromDirectoriesThreaded(directoryPaths, numThreads);
    }
}