package com.aimaster.repository;

import com.aimaster.model.GuestSession;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;
import java.util.Optional;

public interface GuestSessionRepository extends JpaRepository<GuestSession, String> {
    Optional<GuestSession> findByFingerprintHash(String fingerprintHash);
    long countByIpAddressAndCreatedAtAfter(String ipAddress, LocalDateTime since);
}
