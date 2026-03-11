package com.aimaster.controller;

import com.aimaster.model.PromptTemplate;
import com.aimaster.service.ModelCatalogService;
import com.aimaster.service.PromptTemplateService;
import com.aimaster.service.StorageService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@Controller
@RequiredArgsConstructor
public class HistoryController {

    private final StorageService storageService;
    private final ModelCatalogService modelCatalog;
    private final PromptTemplateService templateService;

    @GetMapping("/history")
    public String historyPage(Model model) {
        model.addAttribute("conversations", storageService.getAllConversations());
        model.addAttribute("images", storageService.getAllImages());
        model.addAttribute("videos", storageService.getAllVideos());
        return "history";
    }

    @GetMapping("/templates")
    public String templatesPage(Model model) {
        model.addAttribute("templates", templateService.getAllTemplates());
        model.addAttribute("categories", templateService.getCategories());
        return "templates";
    }

    @PostMapping("/api/templates")
    @ResponseBody
    public ResponseEntity<PromptTemplate> createTemplate(@RequestBody PromptTemplate template) {
        return ResponseEntity.ok(templateService.saveCustomTemplate(template));
    }

    @DeleteMapping("/api/templates/{id}")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> deleteTemplate(@PathVariable String id) {
        templateService.deleteTemplate(id);
        return ResponseEntity.ok(Map.of("success", true));
    }

    @PatchMapping("/api/templates/{id}/favorite")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> toggleFavorite(@PathVariable String id) {
        templateService.toggleFavorite(id);
        return ResponseEntity.ok(Map.of("success", true));
    }

    @GetMapping("/api/templates")
    @ResponseBody
    public ResponseEntity<List<PromptTemplate>> getTemplates(
            @RequestParam(required = false) String type,
            @RequestParam(required = false) String category) {
        List<PromptTemplate> templates;
        if (type != null) {
            templates = templateService.getTemplatesByType(type);
        } else if (category != null) {
            templates = templateService.getTemplatesByCategory(category);
        } else {
            templates = templateService.getAllTemplates();
        }
        return ResponseEntity.ok(templates);
    }

    @GetMapping("/api/models")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> getAllModels() {
        return ResponseEntity.ok(Map.of(
                "text", modelCatalog.getTextModels(),
                "image", modelCatalog.getImageModels(),
                "video", modelCatalog.getVideoModels()
        ));
    }

    @DeleteMapping("/api/history/conversations")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> clearConversations() {
        storageService.clearAllConversations();
        return ResponseEntity.ok(Map.of("success", true));
    }
}
