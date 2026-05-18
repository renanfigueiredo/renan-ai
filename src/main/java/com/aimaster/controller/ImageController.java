package com.aimaster.controller;

import com.aimaster.model.GeneratedImage;
import com.aimaster.model.ImageGenerationRequest;
import com.aimaster.service.ImageGenerationService;
import com.aimaster.service.ModelCatalogService;
import com.aimaster.service.StorageService;
import com.aimaster.service.TextGenerationService;
import com.aimaster.service.UserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;
import java.util.*;

@Slf4j
@Controller
@RequiredArgsConstructor
public class ImageController {

    private final ImageGenerationService imageGenerationService;
    private final ModelCatalogService modelCatalog;
    private final StorageService storageService;
    private final TextGenerationService textGenerationService;
    private final UserService userService;

    private Long getUserId(Principal principal) {
        return userService.findByEmail(principal.getName()).orElseThrow().getId();
    }

    @GetMapping("/image")
    public String imagePage(Model model, Principal principal) {
        Long userId = getUserId(principal);
        model.addAttribute("imageModels", modelCatalog.getImageModels());
        model.addAttribute("imageHistory", storageService.getImagesByUser(userId));
        return "image";
    }

    @PostMapping("/api/image/generate")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> generateImage(@RequestBody ImageGenerationRequest request, Principal principal) {
        try {
            Long userId = getUserId(principal);
            if (request.getPrompt() == null || request.getPrompt().isEmpty()) {
                return ResponseEntity.badRequest().body(Map.of("success", false, "error", "Prompt is required"));
            }
            GeneratedImage result = imageGenerationService.generateImages(request, userId);
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
        } catch (Exception e) {
            log.error("Image generation error", e);
            return ResponseEntity.badRequest().body(Map.of("success", false, "error", e.getMessage()));
        }
    }

    @PostMapping("/api/image/enhance-prompt")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> enhanceImagePrompt(@RequestBody Map<String, String> body) {
        try {
            String enhanced = textGenerationService.enhancePrompt(body.get("prompt"), "image generation");
            return ResponseEntity.ok(Map.of("enhanced", enhanced, "success", true));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("success", false, "error", e.getMessage()));
        }
    }

    @GetMapping("/api/image/history")
    @ResponseBody
    public ResponseEntity<List<GeneratedImage>> getHistory(Principal principal) {
        return ResponseEntity.ok(storageService.getImagesByUser(getUserId(principal)));
    }

    @DeleteMapping("/api/image/{id}")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> deleteImage(@PathVariable String id, Principal principal) {
        boolean ok = storageService.deleteImageByUser(id, getUserId(principal));
        if (!ok) return ResponseEntity.status(404).body(Map.of("success", false));
        return ResponseEntity.ok(Map.of("success", true));
    }

    @PatchMapping("/api/image/{id}/favorite")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> toggleFavorite(@PathVariable String id, Principal principal) {
        boolean ok = storageService.toggleImageFavoriteByUser(id, getUserId(principal));
        if (!ok) return ResponseEntity.status(404).body(Map.of("success", false));
        return ResponseEntity.ok(Map.of("success", true));
    }

    @GetMapping("/api/image/{id}/download")
    public ResponseEntity<byte[]> downloadImage(@PathVariable String id,
                                                  @RequestParam(defaultValue = "0") int index,
                                                  Principal principal) {
        return storageService.findImageByUser(id, getUserId(principal)).map(img -> {
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

