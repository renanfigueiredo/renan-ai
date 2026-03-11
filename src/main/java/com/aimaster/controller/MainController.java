package com.aimaster.controller;

import com.aimaster.model.Conversation;
import com.aimaster.service.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
@RequiredArgsConstructor
public class MainController {

    private final ModelCatalogService modelCatalog;
    private final StorageService storageService;

    @GetMapping("/")
    public String index(Model model) {
        model.addAttribute("textModels", modelCatalog.getTextModels());
        model.addAttribute("imageModels", modelCatalog.getImageModels());
        model.addAttribute("videoModels", modelCatalog.getVideoModels());
        model.addAttribute("recentConversations", storageService.getAllConversations().stream().limit(5).toList());
        model.addAttribute("totalConversations", storageService.getAllConversations().size());
        model.addAttribute("totalImages", storageService.getAllImages().size());
        model.addAttribute("totalVideos", storageService.getAllVideos().size());
        return "index";
    }
}
