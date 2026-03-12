package com.aimaster.controller;

import com.aimaster.model.*;
import com.aimaster.service.*;
import com.vladsch.flexmark.html.HtmlRenderer;
import com.vladsch.flexmark.parser.Parser;
import com.vladsch.flexmark.util.data.MutableDataSet;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.*;

@Slf4j
@Controller
@RequiredArgsConstructor
public class ChatController {

    private final TextGenerationService textGenerationService;
    private final ModelCatalogService modelCatalog;
    private final StorageService storageService;

    @GetMapping("/chat")
    public String chatPage(
            @RequestParam(required = false) String id,
            @RequestParam(required = false) String model,
            Model uiModel) {

        Conversation conversation;
        if (id != null && !id.isEmpty()) {
            conversation = storageService.findConversation(id)
                    .orElseGet(this::newConversation);
        } else {
            conversation = newConversation();
            if (model != null && !model.isEmpty()) {
                conversation.setModelId(model);
            }
        }

        uiModel.addAttribute("conversation", conversation);
        uiModel.addAttribute("textModels", modelCatalog.getTextModels());
        uiModel.addAttribute("defaultModelName", modelCatalog.getModelById(conversation.getModelId())
                .map(m -> m.getName()).orElse("Claude Sonnet 4.6"));
        uiModel.addAttribute("conversations", storageService.getAllConversations());
        return "chat";
    }

    @PostMapping("/api/chat/send")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> sendMessage(@RequestBody ChatRequest request) {
        try {
            // Get or create conversation
            Conversation conversation;
            if (request.getConversationId() != null && !request.getConversationId().isEmpty()) {
                conversation = storageService.findConversation(request.getConversationId())
                        .orElseGet(this::newConversation);
            } else {
                conversation = newConversation();
                if (request.getModelId() != null) {
                    conversation.setModelId(request.getModelId());
                }
                if (request.getSystemPrompt() != null) {
                    conversation.setSystemPrompt(request.getSystemPrompt());
                }
            }

            // Add user message
            String htmlUserContent = "<p>" + escapeHtml(request.getMessage()) + "</p>";
            Message userMsg = Message.builder()
                    .role("user")
                    .content(request.getMessage())
                    .formattedContent(htmlUserContent)
                    .timestamp(LocalDateTime.now())
                    .build();
            conversation.getMessages().add(userMsg);

            // Auto-set conversation title from first message
            if (conversation.getTitle() == null || conversation.getTitle().equals("New Conversation")) {
                String title = request.getMessage().length() > 60
                        ? request.getMessage().substring(0, 60) + "..."
                        : request.getMessage();
                conversation.setTitle(title);
            }

            // Optionally enhance prompt
            String finalMessage = request.getMessage();
            if (request.isEnhancePrompt()) {
                try {
                    finalMessage = textGenerationService.enhancePrompt(request.getMessage(), "chat");
                    request.setMessage(finalMessage);
                } catch (Exception e) {
                    log.warn("Prompt enhancement failed, using original", e);
                }
            }

            // Generate AI response
            Message assistantMsg = textGenerationService.generateResponse(request, conversation);
            conversation.getMessages().add(assistantMsg);

            storageService.saveConversation(conversation);

            Map<String, Object> response = new HashMap<>();
            response.put("conversationId", conversation.getId());
            response.put("userMessage", userMsg);
            response.put("assistantMessage", assistantMsg);
            response.put("conversationTitle", conversation.getTitle());
            response.put("totalTokens", conversation.getTotalTokensUsed());
            response.put("estimatedCost", String.format("$%.6f", conversation.getEstimatedCost()));
            response.put("success", true);

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            log.error("Chat error", e);
            Map<String, Object> error = new HashMap<>();
            error.put("success", false);
            error.put("error", e.getMessage());
            return ResponseEntity.badRequest().body(error);
        }
    }

    @PostMapping("/api/chat/new")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> newConversationApi() {
        Conversation conv = newConversation();
        storageService.saveConversation(conv);
        return ResponseEntity.ok(Map.of("conversationId", conv.getId()));
    }

    @GetMapping("/api/chat/conversation/{id}")
    @ResponseBody
    public ResponseEntity<Conversation> getConversation(@PathVariable String id) {
        return storageService.findConversation(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @DeleteMapping("/api/chat/conversation/{id}")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> deleteConversation(@PathVariable String id) {
        storageService.deleteConversation(id);
        return ResponseEntity.ok(Map.of("success", true));
    }

    @GetMapping("/api/chat/conversations")
    @ResponseBody
    public ResponseEntity<List<Conversation>> listConversations(
            @RequestParam(required = false) String search) {
        List<Conversation> list;
        if (search != null && !search.isEmpty()) {
            list = storageService.searchConversations(search);
        } else {
            list = storageService.getAllConversations();
        }
        // Remove message content for performance, keep only metadata
        List<Map<String, Object>> lightweight = list.stream().map(c -> {
            Map<String, Object> m = new HashMap<>();
            m.put("id", c.getId());
            m.put("title", c.getTitle());
            m.put("modelId", c.getModelId());
            m.put("updatedAt", c.getUpdatedAt());
            m.put("messageCount", c.getMessages().size());
            m.put("pinned", c.isPinned());
            return m;
        }).toList();
        return ResponseEntity.ok(list);
    }

    @PostMapping("/api/chat/enhance-prompt")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> enhancePrompt(@RequestBody Map<String, String> body) {
        try {
            String enhanced = textGenerationService.enhancePrompt(body.get("prompt"), body.getOrDefault("purpose", "chat"));
            return ResponseEntity.ok(Map.of("enhanced", enhanced, "success", true));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("success", false, "error", e.getMessage()));
        }
    }

    @PatchMapping("/api/chat/conversation/{id}/pin")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> togglePin(@PathVariable String id) {
        storageService.findConversation(id).ifPresent(c -> {
            c.setPinned(!c.isPinned());
            storageService.saveConversation(c);
        });
        return ResponseEntity.ok(Map.of("success", true));
    }

    @PatchMapping("/api/chat/conversation/{id}/model")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> changeModel(@PathVariable String id,
                                                            @RequestBody Map<String, String> body) {
        storageService.findConversation(id).ifPresent(c -> {
            c.setModelId(body.get("modelId"));
            storageService.saveConversation(c);
        });
        return ResponseEntity.ok(Map.of("success", true));
    }

    // Model comparison - run same prompt on multiple models
    @PostMapping("/api/chat/compare")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> compareModels(@RequestBody Map<String, Object> body) {
        try {
            String prompt = (String) body.get("prompt");
            String systemPrompt = (String) body.getOrDefault("systemPrompt", "");
            @SuppressWarnings("unchecked")
            List<String> modelIds = (List<String>) body.get("modelIds");

            Map<String, Object> results = new LinkedHashMap<>();
            for (String modelId : modelIds) {
                ChatRequest req = ChatRequest.builder()
                        .modelId(modelId)
                        .message(prompt)
                        .systemPrompt(systemPrompt)
                        .maxTokens(2048)
                        .temperature(0.7)
                        .build();
                Message msg = textGenerationService.generateResponse(req, null);
                results.put(modelId, Map.of(
                        "content", msg.getContent(),
                        "formattedContent", msg.getFormattedContent(),
                        "inputTokens", msg.getInputTokens(),
                        "outputTokens", msg.getOutputTokens(),
                        "latencyMs", msg.getLatencyMs()
                ));
            }

            return ResponseEntity.ok(Map.of("results", results, "success", true));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("success", false, "error", e.getMessage()));
        }
    }

    private Conversation newConversation() {
        return Conversation.builder()
                .title("New Conversation")
                .modelId("us.anthropic.claude-sonnet-4-6")
                .build();
    }

    private String escapeHtml(String text) {
        if (text == null) return "";
        return text.replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace("\"", "&quot;");
    }
}
