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

    /** Public home page — shown to unauthenticated visitors */
    @GetMapping("/")
    public String home(Principal principal) {
        if (principal != null) {
            return "redirect:/dashboard";
        }
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
