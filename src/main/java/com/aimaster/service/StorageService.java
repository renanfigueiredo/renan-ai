package com.aimaster.service;

import com.aimaster.model.Conversation;
import com.aimaster.model.GeneratedImage;
import com.aimaster.model.GeneratedVideo;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class StorageService {

    private final Map<String, Conversation> conversations = new ConcurrentHashMap<>();
    private final List<GeneratedImage> imageHistory = Collections.synchronizedList(new ArrayList<>());
    private final List<GeneratedVideo> videoHistory = Collections.synchronizedList(new ArrayList<>());

    // ========== CONVERSATIONS ==========

    public Conversation saveConversation(Conversation conv) {
        conv.setUpdatedAt(java.time.LocalDateTime.now());
        conversations.put(conv.getId(), conv);
        return conv;
    }

    public Optional<Conversation> findConversation(String id) {
        return Optional.ofNullable(conversations.get(id));
    }

    public List<Conversation> getAllConversations() {
        return conversations.values().stream()
                .sorted(Comparator.comparing(Conversation::getUpdatedAt).reversed())
                .toList();
    }

    public List<Conversation> searchConversations(String query) {
        String lower = query.toLowerCase();
        return conversations.values().stream()
                .filter(c -> c.getTitle().toLowerCase().contains(lower)
                        || c.getMessages().stream().anyMatch(m -> m.getContent().toLowerCase().contains(lower)))
                .sorted(Comparator.comparing(Conversation::getUpdatedAt).reversed())
                .toList();
    }

    public void deleteConversation(String id) {
        conversations.remove(id);
    }

    public void clearAllConversations() {
        conversations.clear();
    }

    // ========== IMAGES ==========

    public void saveImage(GeneratedImage image) {
        imageHistory.add(0, image);
    }

    public List<GeneratedImage> getAllImages() {
        return List.copyOf(imageHistory);
    }

    public Optional<GeneratedImage> findImage(String id) {
        return imageHistory.stream().filter(i -> i.getId().equals(id)).findFirst();
    }

    public void deleteImage(String id) {
        imageHistory.removeIf(i -> i.getId().equals(id));
    }

    public void toggleImageFavorite(String id) {
        imageHistory.stream().filter(i -> i.getId().equals(id))
                .findFirst().ifPresent(i -> i.setFavorite(!i.isFavorite()));
    }

    // ========== VIDEOS ==========

    public void saveVideo(GeneratedVideo video) {
        videoHistory.add(0, video);
    }

    public void updateVideo(GeneratedVideo video) {
        for (int i = 0; i < videoHistory.size(); i++) {
            if (videoHistory.get(i).getId().equals(video.getId())) {
                videoHistory.set(i, video);
                return;
            }
        }
    }

    public Optional<GeneratedVideo> findVideo(String id) {
        return videoHistory.stream().filter(v -> v.getId().equals(id)).findFirst();
    }

    public Optional<GeneratedVideo> findVideoByJobArn(String jobArn) {
        return videoHistory.stream().filter(v -> jobArn.equals(v.getJobArn())).findFirst();
    }

    /** Returns all videos visible in the UI (excludes DOWNLOADED – already consumed). */
    public List<GeneratedVideo> getAllVideos() {
        return videoHistory.stream()
                .filter(v -> !"DOWNLOADED".equals(v.getStatus()))
                .toList();
    }

    /** Used internally by VideoCleanupService for lifecycle management. */
    public List<GeneratedVideo> getAllVideosIncludingDownloaded() {
        return List.copyOf(videoHistory);
    }

    public void deleteVideo(String id) {
        videoHistory.removeIf(v -> v.getId().equals(id));
    }
}
