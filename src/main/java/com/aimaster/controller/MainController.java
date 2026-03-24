package com.aimaster.controller;

import com.aimaster.service.StorageService;
import com.aimaster.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

import java.security.Principal;

@Controller
@RequiredArgsConstructor
public class MainController {

    private final StorageService storageService;
    private final UserService userService;

    /** Public home page — shown to all visitors; sec:authorize in the template handles the authenticated/unauthenticated views */
    @GetMapping("/")
    public String home(Model model) {
        model.addAttribute("activeTab", "portal");
        return "portal/home";
    }

    /** Private dashboard — requires authentication */
    @GetMapping("/dashboard")
    public String dashboard(Model model, Principal principal) {
        Long userId = userService.findByEmail(principal.getName())
                .orElseThrow().getId();
        model.addAttribute("totalConversations", storageService.getConversationsByUser(userId).size());
        model.addAttribute("activeTab", "home");
        return "index";
    }

}
