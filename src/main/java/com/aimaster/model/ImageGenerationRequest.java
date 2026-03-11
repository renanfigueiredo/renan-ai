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
public class ImageGenerationRequest {
    @Builder.Default
    private String id = UUID.randomUUID().toString();
    private String prompt;
    private String negativePrompt;
    private String modelId;
    private int width;
    private int height;
    private int numberOfImages;
    private double cfgScale;
    private long seed;
    private String style;
    private String quality;
    private String referenceImageBase64;
    private double imageStrength;
    @Builder.Default
    private LocalDateTime createdAt = LocalDateTime.now();
}
