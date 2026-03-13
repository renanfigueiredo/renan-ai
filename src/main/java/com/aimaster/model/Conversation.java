package com.aimaster.model;

import com.fasterxml.jackson.annotation.JsonManagedReference;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "conversations")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@ToString(exclude = "messages")
public class Conversation {

    @Id
    @Builder.Default
    private String id = UUID.randomUUID().toString();

    private String title;
    private String modelId;

    @Column(columnDefinition = "TEXT")
    private String systemPrompt;

    /** Owner of this conversation. */
    private Long userId;

    @OneToMany(mappedBy = "conversation", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.EAGER)
    @OrderBy("timestamp ASC")
    @JsonManagedReference
    @Builder.Default
    private List<Message> messages = new ArrayList<>();

    @Builder.Default
    private LocalDateTime createdAt = LocalDateTime.now();

    @Builder.Default
    private LocalDateTime updatedAt = LocalDateTime.now();

    private long totalTokensUsed;
    private double estimatedCost;
    private String category;
    private boolean pinned;

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "conversation_tags", joinColumns = @JoinColumn(name = "conversation_id"))
    @Column(name = "tag")
    @Builder.Default
    private List<String> tags = new ArrayList<>();
}
