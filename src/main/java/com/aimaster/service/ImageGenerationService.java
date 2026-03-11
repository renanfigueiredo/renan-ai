package com.aimaster.service;

import com.aimaster.model.GeneratedImage;
import com.aimaster.model.ImageGenerationRequest;
import com.aimaster.model.ModelInfo;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import software.amazon.awssdk.core.SdkBytes;
import software.amazon.awssdk.services.bedrockruntime.BedrockRuntimeClient;
import software.amazon.awssdk.services.bedrockruntime.model.InvokeModelRequest;
import software.amazon.awssdk.services.bedrockruntime.model.InvokeModelResponse;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

@Slf4j
@Service
@RequiredArgsConstructor
public class ImageGenerationService {

    private final BedrockRuntimeClient bedrockClient;
    private final ModelCatalogService modelCatalog;
    private final StorageService storageService;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public GeneratedImage generateImages(ImageGenerationRequest request) {
        long startTime = System.currentTimeMillis();

        try {
            String modelId = request.getModelId();
            if (modelId == null || modelId.isEmpty()) {
                modelId = "amazon.nova-canvas-v1:0";
            }

            List<String> base64Images;

            if (modelId.contains("stable-image-ultra") || modelId.contains("sd3")) {
                base64Images = invokeStabilityModern(request, modelId);
            } else if (modelId.contains("stability") || modelId.contains("stable-diffusion")) {
                base64Images = invokeStableDiffusion(request, modelId);
            } else if (modelId.contains("titan-image")) {
                base64Images = invokeTitanImage(request, modelId);
            } else if (modelId.contains("nova-canvas")) {
                base64Images = invokeNovaCanvas(request, modelId);
            } else {
                base64Images = invokeNovaCanvas(request, modelId);
            }

            long generationTime = System.currentTimeMillis() - startTime;
            ModelInfo model = modelCatalog.getModelById(modelId).orElse(null);

            GeneratedImage image = GeneratedImage.builder()
                    .prompt(request.getPrompt())
                    .negativePrompt(request.getNegativePrompt())
                    .modelId(modelId)
                    .modelName(model != null ? model.getName() : modelId)
                    .base64Images(base64Images)
                    .width(request.getWidth() > 0 ? request.getWidth() : 1024)
                    .height(request.getHeight() > 0 ? request.getHeight() : 1024)
                    .cfgScale(request.getCfgScale() > 0 ? request.getCfgScale() : 7.5)
                    .seed(request.getSeed())
                    .style(request.getStyle())
                    .generationTimeMs(generationTime)
                    .build();

            storageService.saveImage(image);
            return image;

        } catch (Exception e) {
            log.error("Error generating image", e);
            throw new RuntimeException("Image generation failed: " + e.getMessage(), e);
        }
    }

    // SD3 Large and Stable Image Ultra — new Stability API on Bedrock
    private List<String> invokeStabilityModern(ImageGenerationRequest request, String modelId) throws Exception {
        ObjectNode body = objectMapper.createObjectNode();
        body.put("prompt", request.getPrompt());

        if (request.getNegativePrompt() != null && !request.getNegativePrompt().isEmpty()) {
            body.put("negative_prompt", request.getNegativePrompt());
        }

        // Aspect ratio derived from width/height
        int w = request.getWidth() > 0 ? request.getWidth() : 1024;
        int h = request.getHeight() > 0 ? request.getHeight() : 1024;
        String aspectRatio = deriveAspectRatio(w, h);
        body.put("aspect_ratio", aspectRatio);
        body.put("output_format", "jpeg");

        if (request.getSeed() > 0) {
            body.put("seed", request.getSeed());
        }

        String bodyString = objectMapper.writeValueAsString(body);
        InvokeModelRequest invokeReq = InvokeModelRequest.builder()
                .modelId(modelId)
                .body(SdkBytes.fromString(bodyString, StandardCharsets.UTF_8))
                .contentType("application/json")
                .accept("application/json")
                .build();

        InvokeModelResponse response = bedrockClient.invokeModel(invokeReq);
        JsonNode responseNode = objectMapper.readTree(response.body().asUtf8String());

        List<String> images = new ArrayList<>();
        for (JsonNode img : responseNode.path("images")) {
            images.add(img.asText());
        }
        return images;
    }

    /**
     * Appends style descriptors to the prompt in English so image models honor the style.
     * Nova Canvas and Titan have no native style_preset param — style must be in the text.
     */
    private String applyStyleToPrompt(String prompt, String style) {
        if (style == null || style.isBlank() || "none".equalsIgnoreCase(style)) return prompt;
        String styleDesc = switch (style) {
            case "photographic"  -> "professional photography, photorealistic, DSLR, sharp focus";
            case "cinematic"     -> "cinematic style, dramatic lighting, movie scene, film grain";
            case "digital-art"   -> "digital art, concept art, highly detailed illustration";
            case "anime"         -> "anime style, Japanese animation, vibrant colors, detailed linework";
            case "fantasy-art"   -> "epic fantasy art, magical atmosphere, ArtStation trending, concept art";
            case "line-art"      -> "clean line art, black and white, precise linework, no fill";
            case "comic-book"    -> "comic book style, bold outlines, vibrant flat colors, halftone";
            case "isometric"     -> "isometric view, clean isometric illustration, flat design";
            case "3d-model"      -> "3D render, CGI, octane render, studio lighting, ultra realistic";
            case "neon-punk"     -> "neon punk, cyberpunk aesthetic, neon lights, dark background";
            case "pixel-art"     -> "pixel art, 16-bit, retro game sprite style";
            case "origami"       -> "origami art, paper folding, clean geometric shapes";
            case "watercolor"    -> "watercolor painting, soft edges, painterly style, artistic";
            case "oil-painting"  -> "oil painting, textured canvas, classical painting technique";
            case "low-poly"      -> "low poly art, geometric shapes, faceted, minimalist 3D";
            default              -> style;
        };
        return prompt + ", " + styleDesc;
    }

    private String deriveAspectRatio(int width, int height) {
        if (width == height) return "1:1";
        double ratio = (double) width / height;
        if (ratio > 1.7) return "16:9";
        if (ratio > 1.3) return "3:2";
        if (ratio < 0.6) return "9:16";
        if (ratio < 0.8) return "2:3";
        return "1:1";
    }

    private List<String> invokeStableDiffusion(ImageGenerationRequest request, String modelId) throws Exception {
        ObjectNode body = objectMapper.createObjectNode();

        ArrayNode textPrompts = body.putArray("text_prompts");
        ObjectNode positivePrompt = textPrompts.addObject();
        positivePrompt.put("text", request.getPrompt());
        positivePrompt.put("weight", 1.0);

        if (request.getNegativePrompt() != null && !request.getNegativePrompt().isEmpty()) {
            ObjectNode negPrompt = textPrompts.addObject();
            negPrompt.put("text", request.getNegativePrompt());
            negPrompt.put("weight", -1.0);
        }

        body.put("cfg_scale", request.getCfgScale() > 0 ? request.getCfgScale() : 7.5);
        body.put("steps", 50);
        body.put("width", request.getWidth() > 0 ? request.getWidth() : 1024);
        body.put("height", request.getHeight() > 0 ? request.getHeight() : 1024);
        body.put("samples", Math.min(request.getNumberOfImages() > 0 ? request.getNumberOfImages() : 1, 4));

        if (request.getSeed() > 0) {
            body.put("seed", request.getSeed());
        }

        if (request.getStyle() != null && !request.getStyle().isEmpty() && !"none".equals(request.getStyle())) {
            body.put("style_preset", request.getStyle());
        }

        // Image-to-image
        if (request.getReferenceImageBase64() != null && !request.getReferenceImageBase64().isEmpty()) {
            body.put("init_image", request.getReferenceImageBase64());
            body.put("image_strength", request.getImageStrength() > 0 ? request.getImageStrength() : 0.35);
        }

        String bodyString = objectMapper.writeValueAsString(body);
        InvokeModelRequest invokeReq = InvokeModelRequest.builder()
                .modelId(modelId)
                .body(SdkBytes.fromString(bodyString, StandardCharsets.UTF_8))
                .contentType("application/json")
                .accept("application/json")
                .build();

        InvokeModelResponse response = bedrockClient.invokeModel(invokeReq);
        JsonNode responseNode = objectMapper.readTree(response.body().asUtf8String());

        List<String> images = new ArrayList<>();
        JsonNode artifacts = responseNode.path("artifacts");
        for (JsonNode artifact : artifacts) {
            images.add(artifact.path("base64").asText());
        }
        return images;
    }

    private List<String> invokeTitanImage(ImageGenerationRequest request, String modelId) throws Exception {
        ObjectNode body = objectMapper.createObjectNode();

        // Text-to-image or image-to-image
        if (request.getReferenceImageBase64() != null && !request.getReferenceImageBase64().isEmpty()) {
            body.put("taskType", "IMAGE_VARIATION");
            ObjectNode params = body.putObject("imageVariationParams");
            // Titan Image enforces a 512-char limit on text
            String varPrompt = applyStyleToPrompt(request.getPrompt(), request.getStyle());
            if (varPrompt != null && varPrompt.length() > 512) varPrompt = varPrompt.substring(0, 512);
            if (varPrompt != null && !varPrompt.isEmpty()) params.put("text", varPrompt);
            if (request.getNegativePrompt() != null && !request.getNegativePrompt().isEmpty()) {
                String neg = request.getNegativePrompt();
                if (neg.length() > 512) neg = neg.substring(0, 512);
                params.put("negativeText", neg);
            }
            ArrayNode images = params.putArray("images");
            images.add(request.getReferenceImageBase64());
            params.put("similarityStrength", 1.0 - (request.getImageStrength() > 0 ? request.getImageStrength() : 0.35));
        } else {
            body.put("taskType", "TEXT_IMAGE");
            ObjectNode params = body.putObject("textToImageParams");
            // Titan Image enforces a 512-char limit on text/negativeText
            // Apply style descriptors before truncating
            String titanPrompt = applyStyleToPrompt(request.getPrompt(), request.getStyle());
            if (titanPrompt != null && titanPrompt.length() > 512) {
                titanPrompt = titanPrompt.substring(0, 512);
            }
            params.put("text", titanPrompt);
            if (request.getNegativePrompt() != null && !request.getNegativePrompt().isEmpty()) {
                String negPrompt = request.getNegativePrompt();
                if (negPrompt.length() > 512) negPrompt = negPrompt.substring(0, 512);
                params.put("negativeText", negPrompt);
            }
        }

        ObjectNode imageConfig = body.putObject("imageGenerationConfig");
        imageConfig.put("numberOfImages", Math.min(request.getNumberOfImages() > 0 ? request.getNumberOfImages() : 1, 5));
        imageConfig.put("height", request.getHeight() > 0 ? request.getHeight() : 1024);
        imageConfig.put("width", request.getWidth() > 0 ? request.getWidth() : 1024);
        imageConfig.put("cfgScale", request.getCfgScale() > 0 ? request.getCfgScale() : 8.0);
        if (request.getSeed() > 0) imageConfig.put("seed", request.getSeed());

        String bodyString = objectMapper.writeValueAsString(body);
        InvokeModelRequest invokeReq = InvokeModelRequest.builder()
                .modelId(modelId)
                .body(SdkBytes.fromString(bodyString, StandardCharsets.UTF_8))
                .contentType("application/json")
                .accept("application/json")
                .build();

        InvokeModelResponse response = bedrockClient.invokeModel(invokeReq);
        JsonNode responseNode = objectMapper.readTree(response.body().asUtf8String());

        List<String> images = new ArrayList<>();
        for (JsonNode img : responseNode.path("images")) {
            images.add(img.asText());
        }
        return images;
    }

    private List<String> invokeNovaCanvas(ImageGenerationRequest request, String modelId) throws Exception {
        ObjectNode body = objectMapper.createObjectNode();

        boolean hasReference = request.getReferenceImageBase64() != null
                && !request.getReferenceImageBase64().isEmpty();

        if (hasReference) {
            // Image-to-image: IMAGE_VARIATION mode
            body.put("taskType", "IMAGE_VARIATION");
            ObjectNode params = body.putObject("imageVariationParams");
            ArrayNode images = params.putArray("images");
            images.add(request.getReferenceImageBase64());
            String imgVarPrompt = applyStyleToPrompt(request.getPrompt(), request.getStyle());
            if (imgVarPrompt != null && !imgVarPrompt.isEmpty()) {
                params.put("text", imgVarPrompt);
            }
            if (request.getNegativePrompt() != null && !request.getNegativePrompt().isEmpty()) {
                params.put("negativeText", request.getNegativePrompt());
            }
            // similarityStrength: 0.2 (muito diferente) a 1.0 (muito similar)
            // imageStrength do request = "quanto mudar" => inverter para similarityStrength
            double strength = request.getImageStrength() > 0 ? request.getImageStrength() : 0.5;
            double similarity = Math.max(0.2, Math.min(1.0, 1.0 - strength + 0.2));
            params.put("similarityStrength", similarity);
        } else {
            // Text-to-image normal
            body.put("taskType", "TEXT_IMAGE");
            ObjectNode params = body.putObject("textToImageParams");
            params.put("text", applyStyleToPrompt(request.getPrompt(), request.getStyle()));
            if (request.getNegativePrompt() != null && !request.getNegativePrompt().isEmpty()) {
                params.put("negativeText", request.getNegativePrompt());
            }
        }

        ObjectNode imageConfig = body.putObject("imageGenerationConfig");
        imageConfig.put("numberOfImages", Math.min(request.getNumberOfImages() > 0 ? request.getNumberOfImages() : 1, 5));
        imageConfig.put("height", request.getHeight() > 0 ? request.getHeight() : 1024);
        imageConfig.put("width", request.getWidth() > 0 ? request.getWidth() : 1024);
        imageConfig.put("cfgScale", request.getCfgScale() > 0 ? request.getCfgScale() : 8.0);
        imageConfig.put("quality", request.getQuality() != null ? request.getQuality() : "standard");

        if (request.getSeed() > 0) {
            imageConfig.put("seed", request.getSeed());
        }

        String bodyString = objectMapper.writeValueAsString(body);
        InvokeModelRequest invokeReq = InvokeModelRequest.builder()
                .modelId(modelId)
                .body(SdkBytes.fromString(bodyString, StandardCharsets.UTF_8))
                .contentType("application/json")
                .accept("application/json")
                .build();

        InvokeModelResponse response = bedrockClient.invokeModel(invokeReq);
        JsonNode responseNode = objectMapper.readTree(response.body().asUtf8String());

        List<String> images = new ArrayList<>();
        for (JsonNode img : responseNode.path("images")) {
            images.add(img.asText());
        }
        return images;
    }
}
