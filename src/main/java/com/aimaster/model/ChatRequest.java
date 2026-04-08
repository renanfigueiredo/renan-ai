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
    private Boolean stream;
    private List<String> stopSequences;
    private List<String> imagesBase64;
    private List<String> imagesMimeTypes;
    private List<String> documentsBase64;
    private List<String> documentsNames;
    private Boolean enhancePrompt; // auto-enhance with AI
    private String responseLanguage;
}
