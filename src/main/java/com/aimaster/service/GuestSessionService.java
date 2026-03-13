package com.aimaster.service;

import com.aimaster.model.AppUser;
import com.aimaster.model.GuestSession;
import com.aimaster.model.UserRole;
import com.aimaster.model.UserStatus;
import com.aimaster.repository.GuestSessionRepository;
import com.aimaster.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class GuestSessionService {

    /** Máximo de sessões de visitante por IP nas últimas 24 h */
    private static final int MAX_SESSIONS_PER_IP_PER_DAY = 3;

    /**
     * Delta máximo aceito por heartbeat (segundos).
     * O cliente envia a cada ~25 s; aceitamos até 35 s para folga de rede.
     * Qualquer valor acima é truncado — impede que alguém envie delta=99999.
     */
    private static final int MAX_HEARTBEAT_DELTA = 35;

    private final GuestSessionRepository guestRepo;
    private final UserRepository userRepo;
    private final PasswordEncoder passwordEncoder;

    // ── Criar ou retomar sessão ────────────────────────────────────────────────

    @Transactional
    public GuestSession createOrResumeSession(String fingerprintHash, String ipAddress) {

        // 1. Mesmo fingerprint já existe? Retomar ou bloquear.
        Optional<GuestSession> existing = guestRepo.findByFingerprintHash(fingerprintHash);
        if (existing.isPresent()) {
            GuestSession s = existing.get();
            if (s.isExpired()) {
                throw new IllegalStateException("guest.expired");
            }
            log.info("Resumindo sessão visitante existente id={}", s.getId());
            return s;
        }

        // 2. Rate limit por IP: máx 3 sessões/dia por IP
        long count = guestRepo.countByIpAddressAndCreatedAtAfter(ipAddress, LocalDateTime.now().minusDays(1));
        if (count >= MAX_SESSIONS_PER_IP_PER_DAY) {
            throw new IllegalStateException("guest.rate_limited");
        }

        // 3. Criar AppUser temporário para este visitante
        String sessionId = UUID.randomUUID().toString();
        String guestEmail = "guest_" + sessionId + "@guest.invalid";

        AppUser guestUser = AppUser.builder()
                .name("Visitante")
                .email(guestEmail)
                .password(passwordEncoder.encode(UUID.randomUUID().toString()))
                .role(UserRole.USER)
                .status(UserStatus.ACTIVE)
                .approvalToken(null)
                .createdAt(LocalDateTime.now())
                .build();
        guestUser = userRepo.save(guestUser);

        GuestSession session = GuestSession.builder()
                .id(sessionId)
                .fingerprintHash(fingerprintHash)
                .ipAddress(ipAddress)
                .appUserId(guestUser.getId())
                .elapsedSeconds(0)
                .blocked(false)
                .createdAt(LocalDateTime.now())
                .lastHeartbeat(LocalDateTime.now())
                .build();

        log.info("Nova sessão visitante criada id={} ip={}", sessionId, ipAddress);
        return guestRepo.save(session);
    }

    // ── Heartbeat: cliente informa quanto tempo ficou visível ─────────────────

    @Transactional
    public GuestSession heartbeat(String sessionId, int clientDelta) {
        GuestSession session = guestRepo.findById(sessionId)
                .orElseThrow(() -> new IllegalArgumentException("Sessão não encontrada"));

        if (session.isExpired()) return session;

        // Clamp delta — cliente não pode enviar valor negativo ou absurdamente alto
        int safeDelta = Math.min(Math.max(clientDelta, 0), MAX_HEARTBEAT_DELTA);
        session.setElapsedSeconds(session.getElapsedSeconds() + safeDelta);
        session.setLastHeartbeat(LocalDateTime.now());

        if (session.getElapsedSeconds() >= GuestSession.MAX_SECONDS) {
            session.setElapsedSeconds(GuestSession.MAX_SECONDS);
            session.setBlocked(true);
            // Bloqueia também o AppUser temporário
            userRepo.findById(session.getAppUserId()).ifPresent(u -> {
                u.setStatus(UserStatus.REJECTED);
                userRepo.save(u);
            });
            log.info("Sessão visitante expirada id={}", sessionId);
        }

        return guestRepo.save(session);
    }

    // ── Lookups ───────────────────────────────────────────────────────────────

    public Optional<GuestSession> findValid(String sessionId) {
        return guestRepo.findById(sessionId).filter(s -> !s.isExpired());
    }

    public Optional<GuestSession> findById(String sessionId) {
        return guestRepo.findById(sessionId);
    }
}
