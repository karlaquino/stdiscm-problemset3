package com.garynation;

import java.io.File;
import java.io.FileInputStream;
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
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
