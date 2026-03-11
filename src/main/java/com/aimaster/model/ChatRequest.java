package com.aimaster.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ChatRequest {
    private String conversationId;
    private String message;
    private String modelId;
    private String systemPrompt;
    private double temperature;
    private int maxTokens;
    private double topP;
    private boolean stream;
    private List<String> stopSequences;
    private String imageBase64; // for multimodal
    private String imageMimeType;
    private String documentBase64;
    private String documentName;
    private boolean enhancePrompt; // auto-enhance with AI
    private String responseLanguage;
}
