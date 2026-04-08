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
        Long userId = userService.findByEmail(principal.getName())
                .orElseThrow().getId();

        List<Conversation> convs = storageService.getConversationsByUser(userId);
        model.addAttribute("totalConversations", convs.size());

        // Last conversation for the "resume" card
        if (!convs.isEmpty()) {
            Conversation last = convs.get(0); // already sorted by updatedAt DESC
            model.addAttribute("lastConversation", last);
            // Find last AI message preview
            last.getMessages().stream()
                    .filter(m -> "assistant".equals(m.getRole()))
                    .reduce((a, b) -> b) // last element
                    .ifPresent(m -> {
                        String preview = m.getContent();
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

