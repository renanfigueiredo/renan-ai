package com.aimaster.controller;

import com.aimaster.model.Conversation;
import com.aimaster.service.StorageService;
import com.aimaster.service.UserPreferenceService;
import com.aimaster.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

import java.security.Principal;
import java.util.List;

@Controller
@RequiredArgsConstructor
public class MainController {

    private final StorageService storageService;
    private final UserService userService;
    private final UserPreferenceService userPreferenceService;

    /** Public home page — fast redirect to static HTML (no Thymeleaf processing) */
    @GetMapping("/")
    public String home() {
        return "redirect:/portal/index.html";
    }

    /** Private dashboard — requires authentication */
    @GetMapping("/dashboard")
    public String dashboard(Model model, Principal principal) {
        var userId = userService.findByEmail(principal.getName())
                .orElseThrow().getId();

        var convs = storageService.getConversationsByUser(userId);
        model.addAttribute("totalConversations", convs.size());

        // Last conversation for the "resume" card
        if (!convs.isEmpty()) {
            var last = convs.getFirst(); // already sorted by updatedAt DESC — Java 21+ SequencedCollection
            model.addAttribute("lastConversation", last);
            // Find last AI message preview
            last.getMessages().stream()
                    .filter(m -> "assistant".equals(m.getRole()))
                    .reduce((_, b) -> b) // last element — unnamed variable (Java 22+)
                    .ifPresent(m -> {
                        var preview = m.getContent();
                        if (preview != null && preview.length() > 180) {
                            preview = preview.substring(0, 180) + "…";
                        }
                        model.addAttribute("lastAiPreview", preview);
                    });
        }

        // User preferences for personalisation chips
        model.addAttribute("userPrefs", userPreferenceService.getOrCreate(userId));

        // Record login timestamp
        userPreferenceService.recordLogin(userId);

        model.addAttribute("activeTab", "home");
        return "index";
    }
}

