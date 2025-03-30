package com.garynation.problemset3.consumer_app;

import org.bytedeco.ffmpeg.global.avcodec;
import org.bytedeco.javacv.FFmpegFrameGrabber;
import org.bytedeco.javacv.FFmpegFrameRecorder;
import org.bytedeco.ffmpeg.global.avutil;
import org.bytedeco.javacv.FFmpegLogCallback;
import org.bytedeco.javacv.Frame;
import org.springframework.stereotype.Service;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;

@Service
public class VideoCompressionService {

    /**
     * Compresses a video file and replaces the original file with the compressed version
     *
     * @param inputFilePath The path to the input video file
     * @return The path to the compressed video file (same as input path)
     */
    public String compressVideo(String inputFilePath) {
        // Generate a temporary file path for the compressed output
        String tempOutputFilePath = generateTempOutputFilePath(inputFilePath);
        avutil.av_log_set_level(avutil.AV_LOG_QUIET);
        try (FFmpegFrameGrabber grabber = new FFmpegFrameGrabber(inputFilePath)) {
            grabber.start();

            FFmpegFrameRecorder recorder = new FFmpegFrameRecorder(
                    tempOutputFilePath,
                    grabber.getImageWidth(),
                    grabber.getImageHeight(),
                    grabber.getAudioChannels()
            );

            // Configure compression settings
            recorder.setVideoCodec(avcodec.AV_CODEC_ID_H264);
            recorder.setFormat("mp4");
            recorder.setVideoBitrate(1000); // Lower bitrate for compression
            recorder.setVideoOption("preset", "ultrafast");
            recorder.setVideoQuality(50); // Higher value = lower quality (20-28 is good range)
            recorder.setFrameRate(15);

            // Handle audio if present
            if (grabber.getAudioChannels() > 0) {
                recorder.setAudioCodec(avcodec.AV_CODEC_ID_AAC);
                recorder.setAudioBitrate(128000);
                recorder.setSampleRate(grabber.getSampleRate());
            }

            recorder.start();

            // Process and compress each frame
            Frame frame;
            while ((frame = grabber.grab()) != null) {
                recorder.record(frame);
            }

            recorder.stop();
            recorder.release();

            return tempOutputFilePath; // Return the original path since we replaced the file
        } catch (IOException e) {
//            e.printStackTrace();
            return null;
        }
    }

    private String generateTempOutputFilePath(String inputFilePath) {
        // Create temporary output filename based on input in the same directory
        String extension = inputFilePath.substring(inputFilePath.lastIndexOf('.'));
        String basePath = inputFilePath.substring(0, inputFilePath.lastIndexOf('.'));
        return basePath + "_temp" + extension;
    }
}