package com.aimaster.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ModelInfo {
    private String id;
    private String name;
    private String provider;
    private String category; // TEXT, IMAGE, VIDEO, MULTIMODAL
    private String description;
    private int maxTokens;
    private boolean supportsStreaming;
    private boolean supportsImages;
    private boolean supportsDocuments;
    private boolean supportsSystemPrompt;
    private double inputCostPer1K;
    private double outputCostPer1K;
    private String badgeColor;
    private String icon;
    private boolean available;
}
