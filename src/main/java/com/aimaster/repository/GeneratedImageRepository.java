package com.aimaster.repository;

import com.aimaster.model.GeneratedImage;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface GeneratedImageRepository extends JpaRepository<GeneratedImage, String> {
    List<GeneratedImage> findByUserIdOrderByCreatedAtDesc(Long userId);
    Optional<GeneratedImage> findByIdAndUserId(String id, Long userId);
}
