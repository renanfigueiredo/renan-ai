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
public class Message {
    @Builder.Default
    private String id = UUID.randomUUID().toString();
    private String role; // user, assistant, system
    private String content;
    @Builder.Default
    private LocalDateTime timestamp = LocalDateTime.now();
    private int inputTokens;
    private int outputTokens;
    private long latencyMs;
    private String modelId;
    private boolean isStreaming;
    private String formattedContent; // HTML formatted
}
