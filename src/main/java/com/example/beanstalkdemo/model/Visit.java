package com.example.beanstalkdemo.model;

import jakarta.persistence.*;
import java.time.Instant;

@Entity
@Table(name = "visits")
public class Visit {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String path;
    private String message;

    @Column(name = "visited_at")
    private Instant visitedAt;

    public Visit() {}

    public Visit(String path, String message) {
        this.path = path;
        this.message = message;
        this.visitedAt = Instant.now();
    }

    public Long getId() { return id; }
    public String getPath() { return path; }
    public String getMessage() { return message; }
    public Instant getVisitedAt() { return visitedAt; }

    public void setId(Long id) { this.id = id; }
    public void setPath(String path) { this.path = path; }
    public void setMessage(String message) { this.message = message; }
    public void setVisitedAt(Instant visitedAt) { this.visitedAt = visitedAt; }
}
