package com.aimaster.repository;

import com.aimaster.model.AppUser;
import com.aimaster.model.UserRole;
import com.aimaster.model.UserStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface UserRepository extends JpaRepository<AppUser, Long> {
    Optional<AppUser> findByEmail(String email);
    Optional<AppUser> findByApprovalToken(String token);
    Optional<AppUser> findByResetToken(String resetToken);
    boolean existsByEmail(String email);

    @Query("SELECT u FROM AppUser u WHERE u.email NOT LIKE '%@guest.invalid' ORDER BY u.createdAt DESC")
    List<AppUser> findAllNonGuests();

    @Query("SELECT COUNT(u) FROM AppUser u WHERE u.email NOT LIKE '%@guest.invalid'")
    long countNonGuests();

    @Query("SELECT COUNT(u) FROM AppUser u WHERE u.email NOT LIKE '%@guest.invalid' AND u.status = :status")
    long countNonGuestsByStatus(@Param("status") UserStatus status);

    @Query("SELECT COUNT(u) FROM AppUser u WHERE u.email NOT LIKE '%@guest.invalid' AND u.role = :role")
    long countNonGuestsByRole(@Param("role") UserRole role);
}
