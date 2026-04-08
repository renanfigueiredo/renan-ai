package com.aimaster.service;

import com.aimaster.config.AwsProperties;
import com.aimaster.model.GeneratedVideo;
import com.aimaster.model.VideoGenerationRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class VideoGenerationService {

    private final StorageService storageService;
    private final AsyncVideoWorker asyncVideoWorker;
    private final AwsProperties awsProperties;

    public GeneratedVideo startVideoGeneration(VideoGenerationRequest request, Long userId) {
        var modelId = request.getModelId();
        if (modelId == null || modelId.isEmpty()) modelId = "amazon.nova-reel-v1:0";

        var bucket = request.getS3OutputBucket();
        if (bucket == null || bucket.isEmpty()) bucket = awsProperties.s3().outputBucket();
        if (bucket == null || bucket.isEmpty()) {
            throw new IllegalArgumentException("S3 bucket e obrigatorio para geracao de video (Nova Reel)");
        }

        var video = GeneratedVideo.builder()
                .prompt(request.getPrompt())
                .modelId(modelId)
                .modelName("Amazon Nova Reel")
                .status("PROCESSING")
                .userId(userId)
                .durationSeconds(6)
                .resolution("1280x720")
                .build();

        storageService.saveVideo(video);

        var prefix = "ai-master-videos/" + video.getId() + "/";
        asyncVideoWorker.execute(video, request, modelId, bucket, prefix);

        return video;
    }

    public GeneratedVideo getVideoStatus(String videoId) {
        return storageService.findVideo(videoId)
                .orElseThrow(() -> new RuntimeException("Video not found: " + videoId));
    }
}
