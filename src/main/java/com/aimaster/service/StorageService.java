package com.aimaster.service;

import com.aimaster.model.Conversation;
import com.aimaster.model.GeneratedImage;
import com.aimaster.model.GeneratedVideo;
import com.aimaster.repository.ConversationRepository;
import com.aimaster.repository.GeneratedImageRepository;
import com.aimaster.repository.GeneratedVideoRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class StorageService {

    private final ConversationRepository conversationRepo;
    private final GeneratedImageRepository imageRepo;
    private final GeneratedVideoRepository videoRepo;

    // ========== CONVERSATIONS ==========

    @Transactional
    public Conversation saveConversation(Conversation conv) {
        conv.setUpdatedAt(LocalDateTime.now());
        // wire back-references so cascade works correctly
        if (conv.getMessages() != null) {
            conv.getMessages().forEach(m -> m.setConversation(conv));
        }
        return conversationRepo.save(conv);
    }

    public Optional<Conversation> findConversation(String id) {
        return conversationRepo.findById(id);
    }

    public Optional<Conversation> findConversationByUser(String id, Long userId) {
        return conversationRepo.findByIdAndUserId(id, userId);
    }

    public List<Conversation> getAllConversations() {
        return conversationRepo.findAll();
    }

    public List<Conversation> getConversationsByUser(Long userId) {
        return conversationRepo.findByUserIdOrderByUpdatedAtDesc(userId);
    }

    public List<Conversation> searchConversations(String query) {
        return conversationRepo.findAll().stream()
                .filter(c -> c.getTitle() != null && c.getTitle().toLowerCase().contains(query.toLowerCase()))
                .toList();
    }

    public List<Conversation> searchConversationsByUser(String query, Long userId) {
        return conversationRepo
                .findByUserIdAndTitleContainingIgnoreCaseOrderByUpdatedAtDesc(userId, query);
    }

    @Transactional
    public void deleteConversation(String id) {
        conversationRepo.deleteById(id);
    }

    @Transactional
    public void deleteConversationByUser(String id, Long userId) {
        conversationRepo.deleteByIdAndUserId(id, userId);
    }

    // ========== IMAGES ==========

    @Transactional
    public void saveImage(GeneratedImage image) {
        imageRepo.save(image);
    }

    public List<GeneratedImage> getAllImages() {
        return imageRepo.findAll();
    }

    public List<GeneratedImage> getImagesByUser(Long userId) {
        return imageRepo.findByUserIdOrderByCreatedAtDesc(userId);
    }

    public Optional<GeneratedImage> findImage(String id) {
        return imageRepo.findById(id);
    }

    /** Garante que apenas o dono da imagem consegue acessá-la. */
    public Optional<GeneratedImage> findImageByUser(String id, Long userId) {
        return imageRepo.findByIdAndUserId(id, userId);
    }

    @Transactional
    public void deleteImage(String id) {
        imageRepo.deleteById(id);
    }

    /** Deleta apenas se a imagem pertencer ao usuário informado. */
    @Transactional
    public boolean deleteImageByUser(String id, Long userId) {
        return imageRepo.findByIdAndUserId(id, userId)
                .map(img -> { imageRepo.delete(img); return true; })
                .orElse(false);
    }

    @Transactional
    public void toggleImageFavorite(String id) {
        imageRepo.findById(id).ifPresent(img -> {
            img.setFavorite(!img.isFavorite());
            imageRepo.save(img);
        });
    }

    /** Toggle favorite apenas se o usuário for dono. */
    @Transactional
    public boolean toggleImageFavoriteByUser(String id, Long userId) {
        return imageRepo.findByIdAndUserId(id, userId)
                .map(img -> {
                    img.setFavorite(!img.isFavorite());
                    imageRepo.save(img);
                    return true;
                })
                .orElse(false);
    }

    // ========== VIDEOS ==========

    @Transactional
    public void saveVideo(GeneratedVideo video) {
        videoRepo.save(video);
    }

    @Transactional
    public void updateVideo(GeneratedVideo video) {
        videoRepo.save(video);
    }

    public Optional<GeneratedVideo> findVideo(String id) {
        return videoRepo.findById(id);
    }

    public Optional<GeneratedVideo> findVideoByJobArn(String jobArn) {
        return videoRepo.findByJobArn(jobArn);
    }

    public List<GeneratedVideo> getAllVideos() {
        return videoRepo.findAll().stream()
                .filter(v -> !"DOWNLOADED".equals(v.getStatus()))
                .toList();
    }

    public List<GeneratedVideo> getVideosByUser(Long userId) {
        return videoRepo.findByUserIdOrderByCreatedAtDesc(userId).stream()
                .filter(v -> !"DOWNLOADED".equals(v.getStatus()))
                .toList();
    }

    public List<GeneratedVideo> getAllVideosIncludingDownloaded() {
        return videoRepo.findAll();
    }

    public List<GeneratedVideo> getAllVideosIncludingDownloadedByUser(Long userId) {
        return videoRepo.findByUserId(userId);
    }

    @Transactional
    public void deleteVideo(String id) {
        videoRepo.deleteById(id);
    }
}
