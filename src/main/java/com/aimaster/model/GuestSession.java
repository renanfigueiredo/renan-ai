package com.aimaster.model;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "guest_sessions", indexes = {
        @Index(name = "idx_gs_fingerprint", columnList = "fingerprintHash"),
        @Index(name = "idx_gs_ip",          columnList = "ipAddress")
})
@Getter @Setter @Builder @NoArgsConstructor @AllArgsConstructor
public class GuestSession {

    public static final int MAX_SECONDS = 3600;

    @Id
    private String id; // UUID — também é o valor do cookie GUEST_TOKEN

    /** SHA-256 de componentes do browser (canvas, screen, tz, UA, etc.) */
    @Column(nullable = false, length = 64)
    private String fingerprintHash;

    @Column(length = 64)
    private String ipAddress;

    /** ID do AppUser temporário criado para esta sessão */
    @Column(nullable = false)
    private Long appUserId;

    /** Segundos acumulados ENQUANTO a página estava visível (server-side truth) */
    @Column(nullable = false)
    private int elapsedSeconds;

    @Column(nullable = false)
    private boolean blocked;

    @Column(nullable = false)
    private LocalDateTime createdAt;

    @Column
    private LocalDateTime lastHeartbeat;

    public boolean isExpired() {
        return blocked || elapsedSeconds >= MAX_SECONDS;
    }

    public int getRemainingSeconds() {
        return Math.max(0, MAX_SECONDS - elapsedSeconds);
    }
}
