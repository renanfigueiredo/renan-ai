package com.aimaster.service;

import com.aimaster.config.AwsProperties;
import com.aimaster.model.GeneratedVideo;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import software.amazon.awssdk.services.s3.S3Client;

import java.time.LocalDateTime;

/**
 * Manages the full lifecycle of generated videos:
 *
 *  • purge(video)         – deletes from S3 + removes from in-memory storage (used by delete endpoint)
 *  • scheduleDownload()   – marks video as DOWNLOADED + sets expiresAt = now+5min,
 *                           so the browser can still finish downloading while S3 cleanup is deferred
 *  • Scheduled task       – every 5 minutes, purges:
 *        ① DOWNLOADED videos whose expiresAt has passed
 *        ② COMPLETED videos older than 60 minutes (safety TTL for abandoned generations)
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class VideoCleanupService {

    private final StorageService storageService;
    private final S3Client s3Client;
    private final AwsProperties awsProperties;

    /** Immediately deletes the video from S3 and removes it from the in-memory store. */
    public void purge(GeneratedVideo video) {
        deleteFromS3(video);
        storageService.deleteVideo(video.getId());
        log.info("Video {} purged (S3 + storage)", video.getId());
    }

    /**
     * Marks the video as DOWNLOADED so it disappears from the history API,
     * but gives the browser a 5-minute window to finish the download before S3 deletion.
     */
    public void scheduleDownload(GeneratedVideo video) {
        video.setStatus("DOWNLOADED");
        video.setExpiresAt(LocalDateTime.now().plusMinutes(5));
        storageService.updateVideo(video);
        log.info("Video {} marked DOWNLOADED – S3 cleanup scheduled in ~5 min", video.getId());
    }

    /**
     * Scheduled cleanup – runs every 5 minutes.
     *
     * Purges:
     *   ① DOWNLOADED videos with expiresAt in the past
     *   ② COMPLETED videos completed more than 60 minutes ago (TTL fallback)
     */
    @Scheduled(fixedDelay = 5 * 60 * 1000)
    public void purgeExpiredVideos() {
        var now = LocalDateTime.now();
        var all = storageService.getAllVideosIncludingDownloaded();

        // ① DOWNLOADED with passed expiresAt
        all.stream()
                .filter(v -> "DOWNLOADED".equals(v.getStatus()))
                .filter(v -> v.getExpiresAt() != null && v.getExpiresAt().isBefore(now))
                .forEach(v -> {
                    log.info("Auto-purging downloaded video: {}", v.getId());
                    purge(v);
                });

        // ② COMPLETED older than 60 minutes (browser abandoned without downloading or deleting)
        all.stream()
                .filter(v -> "COMPLETED".equals(v.getStatus()))
                .filter(v -> v.getCompletedAt() != null
                        && v.getCompletedAt().plusMinutes(60).isBefore(now))
                .forEach(v -> {
                    log.info("Auto-purging abandoned video (TTL 60min): {}", v.getId());
                    purge(v);
                });
    }

    // ─── Private helpers ──────────────────────────────────────────────────────

    private void deleteFromS3(GeneratedVideo video) {
        if (video.getS3Uri() == null || video.getS3Uri().isBlank()) return;
        try {
            var parts = parseS3Uri(video.getS3Uri());
            var bucket = parts[0];
            var key    = parts[1] + "output.mp4";

            s3Client.deleteObject(b -> b.bucket(bucket).key(key));
            log.info("Deleted from S3: s3://{}/{}", bucket, key);
        } catch (Exception e) {
            log.warn("Could not delete video {} from S3 (will be lost in bucket): {}",
                    video.getId(), e.getMessage());
        }
    }

    /** Returns [bucket, prefix] where prefix ends with '/'. */
    private String[] parseS3Uri(String s3Uri) {
        var withoutScheme = s3Uri.replace("s3://", "");
        int slashIdx = withoutScheme.indexOf('/');
        var bucket = slashIdx > 0 ? withoutScheme.substring(0, slashIdx) : awsProperties.s3().outputBucket();
        var prefix = slashIdx > 0 ? withoutScheme.substring(slashIdx + 1) : "";
        if (!prefix.endsWith("/")) prefix += "/";
        return new String[]{bucket, prefix};
    }
}
