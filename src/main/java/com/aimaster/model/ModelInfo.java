package com.aimaster.model;

import lombok.Builder;

/**
 * Immutable model descriptor — Java 25 record replaces @Data/@NoArgsConstructor/@AllArgsConstructor.
 * Lombok @Builder still works on records since Lombok 1.18.20+.
 */
@Builder
public record ModelInfo(
        String id,
        String name,
        String provider,
        String category,
        String description,
        int maxTokens,
        boolean supportsStreaming,
        boolean supportsImages,
        boolean supportsDocuments,
        boolean supportsSystemPrompt,
        double inputCostPer1K,
        double outputCostPer1K,
        String badgeColor,
        String icon,
        boolean available
) {}
