package com.aimaster.controller;

import com.aimaster.model.GeneratedImage;
import com.aimaster.model.ImageGenerationRequest;
import com.aimaster.service.ImageGenerationService;
import com.aimaster.service.ModelCatalogService;
import com.aimaster.service.StorageService;
import com.aimaster.service.TextGenerationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.util.*;

@Slf4j
@Controller
@RequiredArgsConstructor
public class ImageController {

    private final ImageGenerationService imageGenerationService;
    private final ModelCatalogService modelCatalog;
    private final StorageService storageService;
    private final TextGenerationService textGenerationService;

    @GetMapping("/image")
    public String imagePage(Model model) {
        model.addAttribute("imageModels", modelCatalog.getImageModels());
        model.addAttribute("imageHistory", storageService.getAllImages());
        return "image";
    }

    @PostMapping("/api/image/generate")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> generateImage(@RequestBody ImageGenerationRequest request) {
        try {
            // Optionally enhance the prompt
            if (request.getPrompt() != null && !request.getPrompt().isEmpty()) {
                GeneratedImage result = imageGenerationService.generateImages(request);
                Map<String, Object> response = new HashMap<>();
                response.put("success", true);
                response.put("id", result.getId());
                response.put("images", result.getBase64Images());
                response.put("prompt", result.getPrompt());
                response.put("modelName", result.getModelName());
                response.put("width", result.getWidth());
                response.put("height", result.getHeight());
                response.put("generationTimeMs", result.getGenerationTimeMs());
                response.put("seed", result.getSeed());
                return ResponseEntity.ok(response);
            } else {
                return ResponseEntity.badRequest().body(Map.of("success", false, "error", "Prompt is required"));
            }
        } catch (Exception e) {
            log.error("Image generation error", e);
            return ResponseEntity.badRequest().body(Map.of("success", false, "error", e.getMessage()));
        }
    }

    @PostMapping("/api/image/enhance-prompt")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> enhanceImagePrompt(@RequestBody Map<String, String> body) {
        try {
            String original = body.get("prompt");
            String enhanced = textGenerationService.enhancePrompt(original, "image generation");
            return ResponseEntity.ok(Map.of("enhanced", enhanced, "success", true));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("success", false, "error", e.getMessage()));
        }
    }

    @GetMapping("/api/image/history")
    @ResponseBody
    public ResponseEntity<List<GeneratedImage>> getHistory() {
        return ResponseEntity.ok(storageService.getAllImages());
    }

    @DeleteMapping("/api/image/{id}")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> deleteImage(@PathVariable String id) {
        storageService.deleteImage(id);
        return ResponseEntity.ok(Map.of("success", true));
    }

    @PatchMapping("/api/image/{id}/favorite")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> toggleFavorite(@PathVariable String id) {
        storageService.toggleImageFavorite(id);
        return ResponseEntity.ok(Map.of("success", true));
    }

    @GetMapping("/api/image/{id}/download")
    public ResponseEntity<byte[]> downloadImage(@PathVariable String id,
                                                  @RequestParam(defaultValue = "0") int index) {
        return storageService.findImage(id).map(img -> {
            String base64 = img.getBase64Images().get(index);
            byte[] imageBytes = Base64.getDecoder().decode(base64);
            return ResponseEntity.ok()
                    .header(HttpHeaders.CONTENT_DISPOSITION,
                            "attachment; filename=\"ai-image-" + id + "-" + index + ".png\"")
                    .contentType(MediaType.IMAGE_PNG)
                    .body(imageBytes);
        }).orElse(ResponseEntity.notFound().build());
    }
}
