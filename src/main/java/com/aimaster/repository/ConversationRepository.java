package com.aimaster.repository;

import com.aimaster.model.Conversation;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface ConversationRepository extends JpaRepository<Conversation, String> {
    List<Conversation> findByUserIdOrderByUpdatedAtDesc(Long userId);
    Optional<Conversation> findByIdAndUserId(String id, Long userId);
    List<Conversation> findByUserIdAndTitleContainingIgnoreCaseOrderByUpdatedAtDesc(Long userId, String title);
    void deleteByIdAndUserId(String id, Long userId);
}
