package com.example.beanstalkdemo.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;

import java.time.Instant;
import java.util.UUID;

@Service
public class S3Service {

    private final S3Client s3;
    private final String bucket;

    public S3Service(
        @Value("${s3.region:us-east-1}") String region,
        @Value("${s3.visits.bucket:}") String bucket
    ) {
        this.s3 = S3Client.builder()
            .region(Region.of(region))
            .build();
        this.bucket = bucket;
    }

    public String recordVisit(String path) {
        if (bucket == null || bucket.isBlank()) {
            return "s3-not-configured";
        }
        String key = "visits/" + UUID.randomUUID() + ".json";
        String body = String.format(
            "{\"path\":\"%s\",\"timestamp\":\"%s\"}",
            path, Instant.now()
        );
        s3.putObject(
            PutObjectRequest.builder()
                .bucket(bucket)
                .key(key)
                .contentType("application/json")
                .build(),
            RequestBody.fromString(body)
        );
        return key;
    }
}
