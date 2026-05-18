package com.aimaster.model;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

/**
 * Stores per-user personalization preferences for the EVJ AI.
 * One row per user, keyed by userId (same PK as app_users.id).
 */
@Entity
@Table(name = "user_preferences")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserPreference {

    /** Shared PK with app_users.id — one preference profile per user. */
    @Id
    private Long userId;

    /**
     * Comma-separated favorite biblical topics chosen during onboarding.
     * e.g. "Salmos,João,Oração,Santidade,Evangelismo"
     */
    @Column(columnDefinition = "TEXT")
    private String favoriteTopics;

    /**
     * Preferred interaction style: CASUAL, DEVOTIONAL, FORMAL, ACADEMIC.
     * Used to adapt AI tone.
     */
    private String interactionStyle;

    /**
     * User-provided suggestions to improve the AI.
     * Stored as free text and shown in the AI's context so it can adapt.
     */
    @Column(columnDefinition = "TEXT")
    private String aiSuggestions;

    /**
     * Custom instructions the user wants the AI to follow.
     * e.g. "Always quote from Psalms when encouraging me"
     */
    @Column(columnDefinition = "TEXT")
    private String customInstructions;

    /**
     * Tradução bíblica preferida — afeta como a IA cita versículos.
     * Valores típicos: ARA, ARC, NAA, NVI, NTLH, ACF, KJA.
     */
    @Column(length = 32)
    private String bibleVersion;

    /**
     * Papel/responsabilidade ministerial do usuário.
     * Valores típicos: PASTOR, LIDER, PROFESSOR_EBD, JOVEM, MEMBRO, INTERESSADO.
     * Usado pela IA para calibrar profundidade e tom.
     */
    @Column(length = 32)
    private String userRole;

    /**
     * Tradição/denominação do usuário — opcional, ajuda a IA a usar exemplos
     * relevantes. Texto livre porque não queremos engessar (ex: "Presbiteriana",
     * "Batista Reformada", "Reformada Continental", "Não-denominacional").
     */
    @Column(length = 64)
    private String denomination;

    /** True once the user completes the onboarding wizard. */
    @Builder.Default
    private boolean onboardingCompleted = false;

    /** Timestamp of the last login — used to compose "welcome back" context. */
    private LocalDateTime lastLoginAt;

    @Builder.Default
    private LocalDateTime createdAt = LocalDateTime.now();

    @Builder.Default
    private LocalDateTime updatedAt = LocalDateTime.now();
}
