package com.example.beanstalkdemo.controller;

import com.example.beanstalkdemo.model.Visit;
import com.example.beanstalkdemo.service.VisitService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
public class AppController {

    @Value("${app.version:unknown}")
    private String version;

    private final VisitService visitService;

    public AppController(VisitService visitService) {
        this.visitService = visitService;
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

    @PostMapping("/visits")
    @ResponseStatus(HttpStatus.CREATED)
    public Visit createVisit(@RequestBody Map<String, String> body) {
        String path = body.getOrDefault("path", "/");
        String message = body.getOrDefault("message", "");
        return visitService.save(path, message);
    }

    @GetMapping("/visits")
    public List<Visit> getVisits() {
        return visitService.findAll();
    }
}
