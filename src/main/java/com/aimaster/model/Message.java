package com.aimaster.model;

import com.fasterxml.jackson.annotation.JsonBackReference;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "messages")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@ToString(exclude = "conversation")
public class Message {

    @Id
    @Builder.Default
    private String id = UUID.randomUUID().toString();

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "conversation_id")
    @JsonBackReference
    private Conversation conversation;

    private String role; // user, assistant, system

    @Column(columnDefinition = "TEXT")
    private String content;

    @Column(columnDefinition = "TEXT")
    private String formattedContent; // HTML formatted

    @Builder.Default
    private LocalDateTime timestamp = LocalDateTime.now();

    private int inputTokens;
    private int outputTokens;
    private long latencyMs;
    private String modelId;
    private boolean streaming;
}
