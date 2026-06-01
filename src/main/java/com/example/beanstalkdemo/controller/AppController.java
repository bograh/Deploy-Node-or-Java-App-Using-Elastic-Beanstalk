package com.example.beanstalkdemo.controller;

import com.example.beanstalkdemo.service.S3Service;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
public class AppController {

    @Value("${app.version:unknown}")
    private String version;

    private final S3Service s3Service;

    public AppController(S3Service s3Service) {
        this.s3Service = s3Service;
    }

    @GetMapping("/")
    public Map<String, String> health() {
        return Map.of(
            "status", "ok",
            "message", "Java app deployed via Elastic Beanstalk"
        );
    }

    @GetMapping("/version")
    public Map<String, String> getVersion() {
        return Map.of(
            "version", version,
            "platform", "AWS Elastic Beanstalk"
        );
    }

    @GetMapping("/visit")
    public Map<String, String> recordVisit() {
        String key = s3Service.recordVisit("/visit");
        return Map.of(
            "status", "recorded",
            "s3Key", key,
            "dataStore", "Amazon S3"
        );
    }
}
