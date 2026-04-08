package com.aimaster.service;

import com.aimaster.model.UserPreference;
import com.aimaster.repository.UserPreferenceRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
public class UserPreferenceService {

    private final UserPreferenceRepository repo;

    /** Returns existing preferences or creates a blank profile for the given user. */
    @Transactional
    public UserPreference getOrCreate(Long userId) {
        return repo.findById(userId).orElseGet(() -> {
            UserPreference pref = UserPreference.builder().userId(userId).build();
            return repo.save(pref);
        });
    }

    @Transactional
    public UserPreference save(UserPreference pref) {
        pref.setUpdatedAt(LocalDateTime.now());
        return repo.save(pref);
    }

    /** Records the current login timestamp for the user. */
    @Transactional
    public void recordLogin(Long userId) {
        UserPreference pref = getOrCreate(userId);
        pref.setLastLoginAt(LocalDateTime.now());
        repo.save(pref);
    }

    /**
     * Builds the personalisation context string to be appended to the AI system prompt.
     * Returns an empty string if the user has no meaningful preferences.
     */
    public String buildAiContext(Long userId) {
        if (userId == null) return "";
        Optional<UserPreference> opt = repo.findById(userId);
        if (opt.isEmpty()) return "";

        UserPreference p = opt.get();
        StringBuilder sb = new StringBuilder();

        if (p.getFavoriteTopics() != null && !p.getFavoriteTopics().isBlank()) {
            sb.append("• Tópicos favoritos do usuário: ").append(p.getFavoriteTopics()).append("\n");
        }

        if (p.getInteractionStyle() != null && !p.getInteractionStyle().isBlank()) {
            String styleDesc = switch (p.getInteractionStyle()) {
                case "DEVOTIONAL" -> "devocional e contemplativo (enfatize aspectos devocionais)";
                case "FORMAL"     -> "formal e teológico (use linguagem precisa e referências doutrinárias)";
                case "ACADEMIC"   -> "acadêmico e aprofundado (aprofunde-se exegeticamente)";
                default           -> "casual e amigável (mantenha tom leve e encorajador)";
            };
            sb.append("• Estilo preferido pelo usuário: ").append(styleDesc).append("\n");
        }

        if (p.getCustomInstructions() != null && !p.getCustomInstructions().isBlank()) {
            sb.append("• Instruções personalizadas do usuário: ").append(p.getCustomInstructions()).append("\n");
        }

        if (p.getAiSuggestions() != null && !p.getAiSuggestions().isBlank()) {
            sb.append("• Feedback do usuário para melhoria: ").append(p.getAiSuggestions()).append("\n");
        }

        if (sb.isEmpty()) return "";

        return "\n\n════════════════════════════════════════\n" +
               "PERFIL PERSONALIZADO DO USUÁRIO\n" +
               "════════════════════════════════════════\n" +
               sb +
               "Adapte seu estilo, tom e exemplos conforme o perfil acima para tornar\n" +
               "a experiência cada vez mais personalizada para este usuário específico.\n";
    }
}
