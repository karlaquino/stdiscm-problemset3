package com.garynation.problemset3.consumer_app.controllers;

import java.io.File;
import java.io.IOException;
import java.util.concurrent.*;
import java.util.stream.Collectors;

import com.garynation.problemset3.consumer_app.VideoCompressionService;
import com.garynation.problemset3.consumer_app.VideoUploadService;
import com.garynation.problemset3.consumer_app.config.VideoUploadProperties;
import jakarta.annotation.PreDestroy;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.method.annotation.MvcUriComponentsBuilder;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@RestController
@RequestMapping("/api/videos")
public class VideoUploadController {

    private final VideoUploadService videoUploadService;

    public VideoUploadController(VideoCompressionService compressionService, VideoUploadProperties properties) {
        // Create a video upload service with 4 threads and a queue size of 10
        this.videoUploadService = new VideoUploadService(compressionService, properties);
    }

    @PostMapping("/upload")
    public ResponseEntity<String> uploadVideo(@RequestParam("file") MultipartFile file) {
        try {
            CompletableFuture<String> future = videoUploadService.uploadVideo(
                    file.getInputStream(),
                    file.getOriginalFilename()
            );

            // Return immediately, processing continues asynchronously
//            return ResponseEntity.accepted().body("Upload started, processing asynchronously");

            // Alternatively, you could wait for the result:
             String fileName = future.get();
             return ResponseEntity.ok("Video uploaded successfully: " + fileName);
        } catch (IOException e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Failed to upload video: " + e.getMessage());
        } catch (RejectedExecutionException e) {
            return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
                    .body("Server busy. Please try again later.");
        } catch (ExecutionException | InterruptedException e) {
            throw new RuntimeException(e);
        }
    }

    @PreDestroy
    public void cleanup() {
        videoUploadService.shutdown();
    }
}