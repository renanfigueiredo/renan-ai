package com.aimaster.model;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "generated_videos")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class GeneratedVideo {

    @Id
    @Builder.Default
    private String id = UUID.randomUUID().toString();

    /** Owner of this video. */
    private Long userId;

    @Column(columnDefinition = "TEXT")
    private String prompt;

    private String modelId;
    private String modelName;
    private String status; // PENDING, PROCESSING, COMPLETED, FAILED, DOWNLOADED
    private String jobArn;
    private String videoUrl;
    private String s3Uri;
    private int durationSeconds;
    private String resolution;
    private long generationTimeMs;

    @Builder.Default
    private LocalDateTime createdAt = LocalDateTime.now();

    private LocalDateTime completedAt;
    private LocalDateTime expiresAt;

    @Column(columnDefinition = "TEXT")
    private String errorMessage;

    private boolean watched;
}
