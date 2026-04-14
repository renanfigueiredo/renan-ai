package com.aimaster.controller;

import com.aimaster.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

import java.security.Principal;

@Controller
@RequestMapping("/admin/users")
@RequiredArgsConstructor
public class UserAdminController {

    private final UserService userService;

    @GetMapping
    public String usersPage(Model model, Principal principal) {
        model.addAttribute("activeTab", "admin-users");
        var stats = userService.getUserStats();
        model.addAttribute("statTotal",   stats.get("total"));
        model.addAttribute("statActive",  stats.get("active"));
        model.addAttribute("statPending", stats.get("pending"));
        model.addAttribute("statAdmin",   stats.get("admin"));
        // Current logged-in user's email — to prevent self-demotion/deletion
        model.addAttribute("currentUserEmail", principal.getName());
        return "admin-users";
    }
}
