package com.garynation.problemset3.consumer_app;

import org.springframework.context.ApplicationEvent;

public class NewVideoEvent extends ApplicationEvent {
    public NewVideoEvent() {
        super("New Video Uploaded");
    }
}