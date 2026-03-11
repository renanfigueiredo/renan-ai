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

        // Claude Sonnet 4.6 (NEWEST - Feb 2026)
        models.add(ModelInfo.builder()
                .id("us.anthropic.claude-sonnet-4-6")
                .name("Claude Sonnet 4.6")
                .provider("Anthropic")
                .category("TEXT")
                .description("Newest Claude model (Feb 2026). Hybrid reasoning, adaptive thinking, efficient code generation, up to 1M tokens beta.")
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

        // Claude 4.5 Haiku
        models.add(ModelInfo.builder()
                .id("us.anthropic.claude-haiku-4-5-20251001-v1:0")
                .name("Claude 4.5 Haiku")
                .provider("Anthropic")
                .category("TEXT")
                .description("Fastest & most cost-effective Claude. Best for real-time tasks.")
                .maxTokens(8192)
                .supportsStreaming(true)
                .supportsImages(true)
                .supportsDocuments(true)
                .supportsSystemPrompt(true)
                .inputCostPer1K(0.0008)
                .outputCostPer1K(0.004)
                .badgeColor("blue")
                .icon("bi-lightning-fill")
                .available(true)
                .build());

        // Claude 4.6 Opus
        models.add(ModelInfo.builder()
                .id("us.anthropic.claude-opus-4-6-v1")
                .name("Claude 4.6 Opus")
                .provider("Anthropic")
                .category("TEXT")
                .description("Most powerful for complex tasks, research, and nuanced reasoning.")
                .maxTokens(4096)
                .supportsStreaming(true)
                .supportsImages(true)
                .supportsDocuments(true)
                .supportsSystemPrompt(true)
                .inputCostPer1K(0.015)
                .outputCostPer1K(0.075)
                .badgeColor("gold")
                .icon("bi-gem")
                .available(true)
                .build());

        // Amazon Nova Pro
        models.add(ModelInfo.builder()
                .id("us.amazon.nova-pro-v1:0")
                .name("Amazon Nova Pro")
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

        // Amazon Nova Lite
        models.add(ModelInfo.builder()
                .id("us.amazon.nova-lite-v1:0")
                .name("Amazon Nova Lite")
                .provider("Amazon")
                .category("TEXT")
                .description("Very low-cost multimodal model. Fast and optimized for simple tasks.")
                .maxTokens(5120)
                .supportsStreaming(true)
                .supportsImages(true)
                .supportsDocuments(false)
                .supportsSystemPrompt(true)
                .inputCostPer1K(0.00006)
                .outputCostPer1K(0.00024)
                .badgeColor("orange")
                .icon("bi-lightning")
                .available(true)
                .build());

        // Amazon Nova Micro
        models.add(ModelInfo.builder()
                .id("us.amazon.nova-micro-v1:0")
                .name("Amazon Nova Micro")
                .provider("Amazon")
                .category("TEXT")
                .description("Most cost-effective text-only model. Ideal for high-volume tasks.")
                .maxTokens(5120)
                .supportsStreaming(true)
                .supportsImages(false)
                .supportsDocuments(false)
                .supportsSystemPrompt(true)
                .inputCostPer1K(0.000035)
                .outputCostPer1K(0.00014)
                .badgeColor("orange")
                .icon("bi-cpu-fill")
                .available(true)
                .build());

        // Meta Llama 4 Maverick 17B Instruct
        models.add(ModelInfo.builder()
                .id("us.meta.llama4-maverick-17b-instruct-v1:0")
                .name("Llama 4 Maverick 17B Instruct")
                .provider("Meta")
                .category("TEXT")
                .description("Meta's largest multimodal instruct model with vision capabilities.")
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

        // Cohere Command R+
        models.add(ModelInfo.builder()
                .id("cohere.command-r-plus-v1:0")
                .name("Command R+")
                .provider("Cohere")
                .category("TEXT")
                .description("Optimized for RAG, summarization, and business use cases.")
                .maxTokens(4096)
                .supportsStreaming(true)
                .supportsImages(false)
                .supportsDocuments(false)
                .supportsSystemPrompt(true)
                .inputCostPer1K(0.003)
                .outputCostPer1K(0.015)
                .badgeColor("teal")
                .icon("bi-intersect")
                .available(true)
                .build());

        // ========== IMAGE MODELS ==========

        // Amazon Nova Canvas
        models.add(ModelInfo.builder()
                .id("amazon.nova-canvas-v1:0")
                .name("Amazon Nova Canvas")
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

        // Stability AI - Stable Image Ultra (not available in us-east-1)
        models.add(ModelInfo.builder()
                .id("stability.stable-image-ultra-v1:0")
                .name("Stable Image Ultra")
                .provider("Stability AI")
                .category("IMAGE")
                .description("Modelo fotorrealista de maior qualidade da Stability AI, com excelente fidelidade ao prompt.")
                .maxTokens(0)
                .supportsStreaming(false)
                .supportsImages(true)
                .supportsDocuments(false)
                .supportsSystemPrompt(false)
                .inputCostPer1K(0.08)
                .outputCostPer1K(0.0)
                .badgeColor("violet")
                .icon("bi-stars")
                .available(false)
                .build());

        // Stability AI - Stable Diffusion 3 Large (not available in us-east-1)
        models.add(ModelInfo.builder()
                .id("stability.sd3-large-v1:0")
                .name("Stable Diffusion 3 Large")
                .provider("Stability AI")
                .category("IMAGE")
                .description("SD3 Large com renderização de texto aprimorada, composição multi-objeto e controle fino.")
                .maxTokens(0)
                .supportsStreaming(false)
                .supportsImages(true)
                .supportsDocuments(false)
                .supportsSystemPrompt(false)
                .inputCostPer1K(0.04)
                .outputCostPer1K(0.0)
                .badgeColor("purple")
                .icon("bi-brush")
                .available(false)
                .build());

        // Amazon Titan Image Generator G2
        models.add(ModelInfo.builder()
                .id("amazon.titan-image-generator-v2:0")
                .name("Titan Image Generator G2")
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
                .name("Amazon Nova Reel")
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
