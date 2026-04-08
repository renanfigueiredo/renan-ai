package com.aimaster.controller;

import com.aimaster.model.UserPreference;
import com.aimaster.service.UserPreferenceService;
import com.aimaster.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;
import java.util.Map;

/**
 * REST API for reading and updating the current user's personalisation preferences.
 * All endpoints require authentication (enforced by SecurityConfig).
 */
@RestController
@RequestMapping("/api/preferences")
@RequiredArgsConstructor
public class UserPreferenceController {

    private final UserPreferenceService preferenceService;
    private final UserService userService;

    /** Simple DTO so we never expose the JPA entity as a request body. */
    record PreferenceInput(
            String favoriteTopics,
            String interactionStyle,
            String aiSuggestions,
            String customInstructions
    ) {}

    private Long getUserId(Principal principal) {
        return userService.findByEmail(principal.getName())
                .orElseThrow(() -> new IllegalStateException("Usuário não encontrado"))
                .getId();
    }

    /** GET /api/preferences — returns a safe view of the current user's preferences. */
    @GetMapping
    public ResponseEntity<Map<String, Object>> get(Principal principal) {
        UserPreference p = preferenceService.getOrCreate(getUserId(principal));
        return ResponseEntity.ok(toMap(p));
    }

    /** POST /api/preferences — update any subset of preference fields. */
    @PostMapping
    public ResponseEntity<Map<String, Object>> update(
            @RequestBody PreferenceInput body, Principal principal) {
        Long userId = getUserId(principal);
        UserPreference pref = preferenceService.getOrCreate(userId);
        if (body.favoriteTopics() != null)    pref.setFavoriteTopics(body.favoriteTopics());
        if (body.interactionStyle() != null && !body.interactionStyle().isBlank())
            pref.setInteractionStyle(body.interactionStyle());
        if (body.aiSuggestions() != null)     pref.setAiSuggestions(body.aiSuggestions());
        if (body.customInstructions() != null) pref.setCustomInstructions(body.customInstructions());
        return ResponseEntity.ok(toMap(preferenceService.save(pref)));
    }

    /**
     * POST /api/preferences/onboarding — complete the onboarding wizard.
     * Saves preferences and marks onboarding as done in one call.
     */
    @PostMapping("/onboarding")
    public ResponseEntity<Map<String, Object>> onboarding(
            @RequestBody PreferenceInput body, Principal principal) {
        Long userId = getUserId(principal);
        UserPreference pref = preferenceService.getOrCreate(userId);
        if (body.favoriteTopics() != null)    pref.setFavoriteTopics(body.favoriteTopics());
        if (body.interactionStyle() != null && !body.interactionStyle().isBlank())
            pref.setInteractionStyle(body.interactionStyle());
        if (body.customInstructions() != null) pref.setCustomInstructions(body.customInstructions());
        pref.setOnboardingCompleted(true);
        preferenceService.save(pref);
        return ResponseEntity.ok(Map.of("success", true, "message", "Bem-vindo(a) à EVJ AI! ✝️"));
    }

    /** Converts entity to a plain Map so Jackson never touches the JPA entity directly. */
    private static Map<String, Object> toMap(UserPreference p) {
        return Map.of(
                "favoriteTopics",     p.getFavoriteTopics()     != null ? p.getFavoriteTopics()     : "",
                "interactionStyle",   p.getInteractionStyle()   != null ? p.getInteractionStyle()   : "",
                "aiSuggestions",      p.getAiSuggestions()      != null ? p.getAiSuggestions()      : "",
                "customInstructions", p.getCustomInstructions() != null ? p.getCustomInstructions() : "",
                "onboardingCompleted", p.isOnboardingCompleted()
        );
    }
}
