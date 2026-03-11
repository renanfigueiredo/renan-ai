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
public class PromptTemplate {
    @Builder.Default
    private String id = UUID.randomUUID().toString();
    private String name;
    private String description;
    private String content;
    private String category; // WRITING, CODING, ANALYSIS, CREATIVE, BUSINESS, etc.
    private String type; // TEXT, IMAGE, VIDEO
    private String icon;
    private List<String> tags;
    private boolean isBuiltIn;
    private boolean isFavorite;
    @Builder.Default
    private LocalDateTime createdAt = LocalDateTime.now();
    private int usageCount;
}
