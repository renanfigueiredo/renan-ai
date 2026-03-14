package com.aimaster.repository;

import com.aimaster.model.AppUser;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface UserRepository extends JpaRepository<AppUser, Long> {
    Optional<AppUser> findByEmail(String email);
    Optional<AppUser> findByApprovalToken(String token);
    Optional<AppUser> findByResetToken(String resetToken);
    boolean existsByEmail(String email);
}
