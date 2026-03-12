package com.aimaster.controller;

import com.aimaster.service.StorageService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
@RequiredArgsConstructor
public class MainController {

    private final StorageService storageService;

    @GetMapping("/")
    public String index(Model model) {
        model.addAttribute("totalConversations", storageService.getAllConversations().size());
        model.addAttribute("totalImages", storageService.getAllImages().size());
        model.addAttribute("totalVideos", storageService.getAllVideos().size());
        return "index";
    }
}
