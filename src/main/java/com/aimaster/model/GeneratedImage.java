package com.aimaster.model;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "generated_images")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class GeneratedImage {

    @Id
    @Builder.Default
    private String id = UUID.randomUUID().toString();

    /** Owner of this image. */
    private Long userId;

    @Column(columnDefinition = "TEXT")
    private String prompt;

    @Column(columnDefinition = "TEXT")
    private String negativePrompt;

    private String modelId;
    private String modelName;
    private int width;
    private int height;
    private double cfgScale;
    private long seed;
    private String style;
    private long generationTimeMs;

    @Builder.Default
    private LocalDateTime createdAt = LocalDateTime.now();

    private boolean favorite;

    /** Base64-encoded images – stored as TEXT per image. */
    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "generated_image_data", joinColumns = @JoinColumn(name = "image_id"))
    @Column(name = "base64_data", columnDefinition = "TEXT")
    @Builder.Default
    private List<String> base64Images = new ArrayList<>();
}
