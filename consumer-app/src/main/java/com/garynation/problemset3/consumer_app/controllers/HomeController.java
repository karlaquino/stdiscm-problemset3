package com.garynation.problemset3.consumer_app.controllers;

import com.garynation.problemset3.consumer_app.NewVideoEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.ResponseBody;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

@Controller
public class HomeController {

    private List<String> videoFiles = new ArrayList<>();

    @GetMapping("/")
    public String home(Model model) {
        updateVideoList();
        model.addAttribute("videos", videoFiles);
        return "home";
    }

    @GetMapping("/videos")
    @ResponseBody
    public List<String> getVideoList() {
        updateVideoList();
        return videoFiles;
    }

    @EventListener
    public void handleNewVideo(NewVideoEvent event) {
        updateVideoList();
    }

    private void updateVideoList() {
        videoFiles.clear();
        File dir = new File(VideoHandler.getSaveDirectory());
        File[] files = dir.listFiles((d, name) -> name.endsWith(".mp4") || name.endsWith(".avi") || name.endsWith(".mov"));
        if (files != null) {
            for (File file : files) {
                videoFiles.add(file.getName());
            }
        }
    }
}