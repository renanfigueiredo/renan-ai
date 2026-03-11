package com.aimaster.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class GeneratedImage {
    @Builder.Default
    private String id = UUID.randomUUID().toString();
    private String prompt;
    private String negativePrompt;
    private String modelId;
    private String modelName;
    private List<String> base64Images; // base64 encoded images
    private int width;
    private int height;
    private double cfgScale;
    private long seed;
    private String style;
    private long generationTimeMs;
    @Builder.Default
    private LocalDateTime createdAt = LocalDateTime.now();
    private boolean favorite;
}
