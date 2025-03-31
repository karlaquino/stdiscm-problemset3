package com.garynation.problemset3.consumer_app.config;

import jakarta.validation.constraints.Min;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import org.springframework.stereotype.Component;
import org.springframework.validation.annotation.Validated;

@Component
@Configuration
@ConfigurationProperties(prefix = "video.upload")
@Validated
public class VideoUploadProperties {

    @Min(value = 1, message = "Thread pool size must be a number and at least 1.")
    private int threadPoolSize = 4; // Default value

    @Min(value = 1, message = "Queue size must be a number and at least 1.")
    private int queueSize = 10;     // Default value

    public int getThreadPoolSize() {
        return threadPoolSize;
    }

    public void setThreadPoolSize(int threadPoolSize) {
        this.threadPoolSize = threadPoolSize;
    }

    public int getQueueSize() {
        return queueSize;
    }

    public void setQueueSize(int queueSize) {
        this.queueSize = queueSize;
    }
}