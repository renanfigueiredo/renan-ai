package com.aimaster.controller;

import com.aimaster.model.GeneratedVideo;
import com.aimaster.model.VideoGenerationRequest;
import com.aimaster.service.ModelCatalogService;
import com.aimaster.service.PromptTemplateService;
import com.aimaster.service.StorageService;
import com.aimaster.service.VideoCleanupService;
import com.aimaster.service.VideoGenerationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import software.amazon.awssdk.services.s3.presigner.model.GetObjectPresignRequest;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;

import java.net.URI;
import java.time.Duration;
import java.util.*;

@Slf4j
@Controller
@RequiredArgsConstructor
public class VideoController {

    private final VideoGenerationService videoGenerationService;
    private final ModelCatalogService modelCatalog;
    private final StorageService storageService;
    private final PromptTemplateService promptTemplateService;
    private final S3Presigner s3Presigner;
    private final VideoCleanupService videoCleanupService;

    @Value("${aws.s3.output-bucket:}")
    private String defaultBucket;

    @GetMapping("/video")
    public String videoPage(Model model) {
        model.addAttribute("videoModels", modelCatalog.getVideoModels());
        model.addAttribute("videoHistory", storageService.getAllVideos());
        model.addAttribute("templates", promptTemplateService.getTemplatesByType("VIDEO"));
        return "video";
    }

    @PostMapping("/api/video/generate")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> generateVideo(@RequestBody VideoGenerationRequest request) {
        try {
            GeneratedVideo video = videoGenerationService.startVideoGeneration(request);
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("id", video.getId());
            response.put("status", video.getStatus());
            response.put("message", "Video generation started. This may take a few minutes.");
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("Video generation error", e);
            return ResponseEntity.badRequest().body(Map.of("success", false, "error", e.getMessage()));
        }
    }

    @GetMapping("/api/video/{id}/status")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> getVideoStatus(@PathVariable String id) {
        return storageService.findVideo(id).map(video -> {
            Map<String, Object> response = new HashMap<>();
            response.put("id", video.getId());
            response.put("status", video.getStatus());
            response.put("prompt", video.getPrompt());
            response.put("modelName", video.getModelName());
            response.put("durationSeconds", video.getDurationSeconds());
            response.put("s3Uri", video.getS3Uri());
            response.put("errorMessage", video.getErrorMessage());
            response.put("generationTimeMs", video.getGenerationTimeMs());
            response.put("createdAt", video.getCreatedAt());
            response.put("completedAt", video.getCompletedAt());
            return ResponseEntity.ok(response);
        }).orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/api/video/{id}/download-url")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> getDownloadUrl(@PathVariable String id) {
        return storageService.findVideo(id).map(video -> {
            try {
                if (video.getS3Uri() == null) {
                    return ResponseEntity.badRequest()
                            .<Map<String, Object>>body(Map.of("error", "Vídeo ainda não está disponível"));
                }

                // s3Uri format: s3://bucket/prefix/
                String s3Uri = video.getS3Uri();
                String withoutScheme = s3Uri.replace("s3://", "");
                int slashIdx = withoutScheme.indexOf('/');
                String bucket = slashIdx > 0 ? withoutScheme.substring(0, slashIdx) : defaultBucket;
                String prefix = slashIdx > 0 ? withoutScheme.substring(slashIdx + 1) : "";
                if (!prefix.endsWith("/")) prefix += "/";
                String key = prefix + "output.mp4";

                String presignedUrl = s3Presigner.presignGetObject(GetObjectPresignRequest.builder()
                        .signatureDuration(Duration.ofMinutes(10))
                        .getObjectRequest(GetObjectRequest.builder()
                                .bucket(bucket)
                                .key(key)
                                .build())
                        .build()).url().toString();

                // Mark as DOWNLOADED – hides from UI and schedules S3 cleanup in 5 min
                videoCleanupService.scheduleDownload(video);

                return ResponseEntity.ok(Map.<String, Object>of("url", presignedUrl));
            } catch (Exception e) {
                log.error("Error generating presigned URL for video {}", id, e);
                return ResponseEntity.internalServerError()
                        .<Map<String, Object>>body(Map.of("error", e.getMessage()));
            }
        }).orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/api/video/history")
    @ResponseBody
    public ResponseEntity<List<GeneratedVideo>> getHistory() {
        return ResponseEntity.ok(storageService.getAllVideos());
    }

    @DeleteMapping("/api/video/{id}")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> deleteVideo(@PathVariable String id) {
        storageService.findVideo(id).ifPresentOrElse(
                videoCleanupService::purge,
                () -> storageService.deleteVideo(id) // fallback: remove from storage even if not found properly
        );
        return ResponseEntity.ok(Map.of("success", true));
    }
}
