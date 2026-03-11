package com.aimaster.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Conversation {
    @Builder.Default
    private String id = UUID.randomUUID().toString();
    private String title;
    private String modelId;
    private String systemPrompt;
    @Builder.Default
    private List<Message> messages = new ArrayList<>();
    @Builder.Default
    private LocalDateTime createdAt = LocalDateTime.now();
    @Builder.Default
    private LocalDateTime updatedAt = LocalDateTime.now();
    private long totalTokensUsed;
    private double estimatedCost;
    private String category;
    private List<String> tags;
    private boolean pinned;
}
