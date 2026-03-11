package com.aimaster.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class GeneratedVideo {
    @Builder.Default
    private String id = UUID.randomUUID().toString();
    private String prompt;
    private String modelId;
    private String modelName;
    private String status; // PENDING, PROCESSING, COMPLETED, FAILED, DOWNLOADED
    private String jobArn;
    private String videoUrl;
    private String s3Uri;
    private int durationSeconds;
    private String resolution;
    private long generationTimeMs;
    @Builder.Default
    private LocalDateTime createdAt = LocalDateTime.now();
    private LocalDateTime completedAt;
    private LocalDateTime expiresAt;
    private String errorMessage;
    private boolean watched;
}
