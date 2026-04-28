package com.aimaster.repository;

import com.aimaster.model.CourseRegistration;
import com.aimaster.model.CourseRegistrationStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface CourseRegistrationRepository extends JpaRepository<CourseRegistration, Long> {

    List<CourseRegistration> findByCourseKeyOrderByCreatedAtDesc(String courseKey);

    boolean existsByCourseKeyAndEmail(String courseKey, String email);

    long countByCourseKeyAndStatus(String courseKey, CourseRegistrationStatus status);

    long countByCourseKey(String courseKey);

    Optional<CourseRegistration> findByCourseKeyAndId(String courseKey, Long id);
}
