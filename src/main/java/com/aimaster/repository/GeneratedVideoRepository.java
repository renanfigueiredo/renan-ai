package com.aimaster.repository;

import com.aimaster.model.GeneratedVideo;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface GeneratedVideoRepository extends JpaRepository<GeneratedVideo, String> {
    List<GeneratedVideo> findByUserIdOrderByCreatedAtDesc(Long userId);
    Optional<GeneratedVideo> findByIdAndUserId(String id, Long userId);
    Optional<GeneratedVideo> findByJobArn(String jobArn);
    List<GeneratedVideo> findByUserId(Long userId);
}
