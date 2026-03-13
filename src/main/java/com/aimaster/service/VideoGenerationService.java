package com.aimaster.service;

import com.aimaster.model.GeneratedVideo;
import com.aimaster.model.VideoGenerationRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class VideoGenerationService {

    private final StorageService storageService;
    private final AsyncVideoWorker asyncVideoWorker;

    @Value("${aws.s3.output-bucket:}")
    private String defaultBucket;

    public GeneratedVideo startVideoGeneration(VideoGenerationRequest request, Long userId) {
        String modelId = request.getModelId();
        if (modelId == null || modelId.isEmpty()) modelId = "amazon.nova-reel-v1:0";

        String bucket = request.getS3OutputBucket();
        if (bucket == null || bucket.isEmpty()) bucket = defaultBucket;
        if (bucket == null || bucket.isEmpty()) {
            throw new IllegalArgumentException("S3 bucket e obrigatorio para geracao de video (Nova Reel)");
        }

        GeneratedVideo video = GeneratedVideo.builder()
                .prompt(request.getPrompt())
                .modelId(modelId)
                .modelName("Amazon Nova Reel")
                .status("PROCESSING")
                .userId(userId)
                .durationSeconds(6)
                .resolution("1280x720")
                .build();

        storageService.saveVideo(video);

        String prefix = "ai-master-videos/" + video.getId() + "/";
        asyncVideoWorker.execute(video, request, modelId, bucket, prefix);

        return video;
    }

    public GeneratedVideo getVideoStatus(String videoId) {
        return storageService.findVideo(videoId)
                .orElseThrow(() -> new RuntimeException("Video not found: " + videoId));
    }
}
