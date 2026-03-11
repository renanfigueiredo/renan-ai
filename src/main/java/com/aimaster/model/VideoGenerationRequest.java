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
public class VideoGenerationRequest {
    @Builder.Default
    private String id = UUID.randomUUID().toString();
    private String prompt;
    private String modelId;
    private int durationSeconds;
    private String resolution;
    private double fps;
    private String referenceImageBase64;
    private String s3OutputBucket;
    private String s3OutputPrefix;
    @Builder.Default
    private LocalDateTime createdAt = LocalDateTime.now();
}
