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
        // ========== TEXT MODELS ==========

        // Claude Sonnet 4.6
        models.add(ModelInfo.builder()
                .id("us.anthropic.claude-sonnet-4-6")
                .name("Claude Sonnet")
                .provider("Anthropic")
                .category("TEXT")
                .description("Newest Claude model. Hybrid reasoning, adaptive thinking, efficient code generation.")
                .maxTokens(16000)
                .supportsStreaming(true)
                .supportsImages(true)
                .supportsDocuments(true)
                .supportsSystemPrompt(true)
                .inputCostPer1K(0.003)
                .outputCostPer1K(0.015)
                .badgeColor("purple")
                .icon("bi-stars")
                .available(true)
                .build());

        // Amazon Nova Pro
        models.add(ModelInfo.builder()
                .id("us.amazon.nova-pro-v1:0")
                .name("Nova Pro")
                .provider("Amazon")
                .category("TEXT")
                .description("Amazon's most capable multimodal model with best accuracy-speed-cost balance.")
                .maxTokens(5120)
                .supportsStreaming(true)
                .supportsImages(true)
                .supportsDocuments(true)
                .supportsSystemPrompt(true)
                .inputCostPer1K(0.0008)
                .outputCostPer1K(0.0032)
                .badgeColor("orange")
                .icon("bi-amazon")
                .available(true)
                .build());

        // Meta Llama 4 Maverick 17B Instruct
        models.add(ModelInfo.builder()
                .id("us.meta.llama4-maverick-17b-instruct-v1:0")
                .name("Llama")
                .provider("Meta")
                .category("TEXT")
                .description("Meta's multimodal instruct model with vision capabilities.")
                .maxTokens(4096)
                .supportsStreaming(true)
                .supportsImages(true)
                .supportsDocuments(false)
                .supportsSystemPrompt(true)
                .inputCostPer1K(0.002)
                .outputCostPer1K(0.002)
                .badgeColor("blue")
                .icon("bi-meta")
                .available(true)
                .build());

        // ========== IMAGE MODELS ==========

        // Amazon Nova Canvas
        models.add(ModelInfo.builder()
                .id("amazon.nova-canvas-v1:0")
                .name("Canvas")
                .provider("Amazon")
                .category("IMAGE")
                .description("Modelo de geração de imagens de última geração da Amazon com controles avançados.")
                .maxTokens(0)
                .supportsStreaming(false)
                .supportsImages(true)
                .supportsDocuments(false)
                .supportsSystemPrompt(false)
                .inputCostPer1K(0.04)
                .outputCostPer1K(0.0)
                .badgeColor("amber")
                .icon("bi-image")
                .available(true)
                .build());

        // Amazon Titan Image Generator G2
        models.add(ModelInfo.builder()
                .id("amazon.titan-image-generator-v2:0")
                .name("Titan")
                .provider("Amazon")
                .category("IMAGE")
                .description("Geração de imagens personalizável com suporte a inpainting, outpainting e variações.")
                .maxTokens(0)
                .supportsStreaming(false)
                .supportsImages(true)
                .supportsDocuments(false)
                .supportsSystemPrompt(false)
                .inputCostPer1K(0.01)
                .outputCostPer1K(0.0)
                .badgeColor("orange")
                .icon("bi-card-image")
                .available(true)
                .build());

        // ========== VIDEO MODELS ==========

        // Amazon Nova Reel
        models.add(ModelInfo.builder()
                .id("amazon.nova-reel-v1:0")
                .name("Reel")
                .provider("Amazon")
                .category("VIDEO")
                .description("Amazon's video generation model. Create videos from text or image prompts.")
                .maxTokens(0)
                .supportsStreaming(false)
                .supportsImages(true)
                .supportsDocuments(false)
                .supportsSystemPrompt(false)
                .inputCostPer1K(0.8)
                .outputCostPer1K(0.0)
                .badgeColor("red")
                .icon("bi-film")
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
