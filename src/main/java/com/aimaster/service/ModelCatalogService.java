package com.aimaster.service;

import com.aimaster.model.ModelInfo;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Service
public class ModelCatalogService {

    private final List<ModelInfo> models = new ArrayList<>();

    public ModelCatalogService() {
        initializeModels();
    }

    private void initializeModels() {
        // EVJ AI — somente Claude Sonnet
        models.add(ModelInfo.builder()
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
                .build());
    }

    public List<ModelInfo> getAllModels() {
        return models;
    }

    public List<ModelInfo> getTextModels() {
        return models.stream()
                .filter(m -> "TEXT".equals(m.getCategory()) && m.isAvailable())
                .toList();
    }

    public List<ModelInfo> getImageModels() {
        return models.stream()
                .filter(m -> "IMAGE".equals(m.getCategory()) && m.isAvailable())
                .toList();
    }

    public List<ModelInfo> getVideoModels() {
        return models.stream()
                .filter(m -> "VIDEO".equals(m.getCategory()) && m.isAvailable())
                .toList();
    }

    public Optional<ModelInfo> getModelById(String id) {
        return models.stream().filter(m -> m.getId().equals(id)).findFirst();
    }
}
