package com.aimaster.model;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "course_registrations",
       uniqueConstraints = @UniqueConstraint(columnNames = {"course_key", "email"}))
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CourseRegistration {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** Identificador do curso (ex.: "namoro-com-proposito"). */
    @Column(name = "course_key", nullable = false, length = 80)
    private String courseKey;

    @Column(nullable = false)
    private String name;

    @Column(nullable = false)
    private String email;

    /** WhatsApp ou telefone (opcional). */
    private String phone;

    @Enumerated(EnumType.STRING)
    @Builder.Default
    private CourseRegistrationStatus status = CourseRegistrationStatus.REGISTERED;

    /** Observações internas do admin (opcional). */
    @Column(length = 500)
    private String notes;

    @Builder.Default
    private LocalDateTime createdAt = LocalDateTime.now();
}
