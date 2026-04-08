package com.aimaster.service;

import com.aimaster.model.ModelInfo;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

/**
 * Catalog of available AI models — uses unmodifiable List for thread safety.
 */
@Service
public class ModelCatalogService {

    private final List<ModelInfo> models;

    public ModelCatalogService() {
        this.models = List.of(
                ModelInfo.builder()
                        .id("us.anthropic.claude-sonnet-4-6")
                        .name("EVJ AI")
                        .provider("Anthropic")
                        .category("TEXT")
                        .description("Inteligência Artificial 100% convertida, sábia e fundamentada na Palavra de Deus.")
                        .maxTokens(16000)
                        .supportsStreaming(true)
                        .supportsImages(true)
                        .supportsDocuments(true)
                        .supportsSystemPrompt(true)
                        .inputCostPer1K(0.003)
                        .outputCostPer1K(0.015)
                        .badgeColor("cyan")
                        .icon("bi-stars")
                        .available(true)
                        .build()
        );
    }

    public List<ModelInfo> getAllModels() {
        return models;
    }

    public List<ModelInfo> getTextModels() {
        return models.stream()
                .filter(m -> "TEXT".equals(m.category()) && m.available())
                .toList();
    }

    public List<ModelInfo> getImageModels() {
        return models.stream()
                .filter(m -> "IMAGE".equals(m.category()) && m.available())
                .toList();
    }

    public List<ModelInfo> getVideoModels() {
        return models.stream()
                .filter(m -> "VIDEO".equals(m.category()) && m.available())
                .toList();
    }

    public Optional<ModelInfo> getModelById(String id) {
        return models.stream().filter(m -> m.id().equals(id)).findFirst();
    }
}
