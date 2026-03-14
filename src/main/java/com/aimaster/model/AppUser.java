package com.aimaster.model;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "app_users")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@ToString(exclude = "password")
public class AppUser {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String name;

    @Column(nullable = false, unique = true)
    private String email;

    @Column(nullable = false)
    private String password;

    @Enumerated(EnumType.STRING)
    @Builder.Default
    private UserRole role = UserRole.USER;

    @Enumerated(EnumType.STRING)
    @Builder.Default
    private UserStatus status = UserStatus.PENDING;

    /** UUID token stored in approval/rejection email links. */
    private String approvalToken;

    /** Token gerado para reset de senha (SHA-256 hex do UUID enviado por e-mail). */
    @Column(length = 64)
    private String resetToken;

    /** Expiração do token de reset (1 hora após geração). */
    private LocalDateTime resetTokenExpiry;

    @Builder.Default
    private LocalDateTime createdAt = LocalDateTime.now();
}
