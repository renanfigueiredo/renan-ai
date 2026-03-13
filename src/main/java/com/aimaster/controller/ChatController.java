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

import java.security.Principal;
import java.time.LocalDateTime;
import java.util.*;

@Slf4j
@Controller
@RequiredArgsConstructor
public class ChatController {

    private final TextGenerationService textGenerationService;
    private final ModelCatalogService modelCatalog;
    private final StorageService storageService;
    private final UserService userService;

    private Long getUserId(Principal principal) {
        return userService.findByEmail(principal.getName())
                .orElseThrow(() -> new IllegalStateException("Usuário não encontrado"))
                .getId();
    }

    @GetMapping("/chat")
    public String chatPage(
            @RequestParam(required = false) String id,
            @RequestParam(required = false) String model,
            Model uiModel,
            Principal principal) {

        Long userId = getUserId(principal);

        Conversation conversation;
        if (id != null && !id.isEmpty()) {
            conversation = storageService.findConversationByUser(id, userId)
                    .orElseGet(() -> newConversation(userId));
        } else {
            conversation = newConversation(userId);
            if (model != null && !model.isEmpty()) {
                conversation.setModelId(model);
            }
        }

        uiModel.addAttribute("conversation", conversation);
        uiModel.addAttribute("textModels", modelCatalog.getTextModels());
        uiModel.addAttribute("defaultModelName", modelCatalog.getModelById(conversation.getModelId())
                .map(m -> m.getName()).orElse("Claude Sonnet 4.6"));
        uiModel.addAttribute("conversations", storageService.getConversationsByUser(userId));
        return "chat";
    }

    @PostMapping("/api/chat/send")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> sendMessage(@RequestBody ChatRequest request, Principal principal) {
        try {
            Long userId = getUserId(principal);

            Conversation conversation;
            if (request.getConversationId() != null && !request.getConversationId().isEmpty()) {
                conversation = storageService.findConversationByUser(request.getConversationId(), userId)
                        .orElseGet(() -> newConversation(userId));
            } else {
                conversation = newConversation(userId);
                if (request.getModelId() != null) conversation.setModelId(request.getModelId());
                if (request.getSystemPrompt() != null) conversation.setSystemPrompt(request.getSystemPrompt());
            }

            String htmlUserContent = "<p>" + escapeHtml(request.getMessage()) + "</p>";
            Message userMsg = Message.builder()
                    .role("user")
                    .content(request.getMessage())
                    .formattedContent(htmlUserContent)
                    .timestamp(LocalDateTime.now())
                    .build();
            userMsg.setConversation(conversation);
            conversation.getMessages().add(userMsg);

            if (conversation.getTitle() == null || conversation.getTitle().equals("New Conversation")) {
                String title = request.getMessage().length() > 60
                        ? request.getMessage().substring(0, 60) + "..."
                        : request.getMessage();
                conversation.setTitle(title);
            }

            if (request.isEnhancePrompt()) {
                try {
                    request.setMessage(textGenerationService.enhancePrompt(request.getMessage(), "chat"));
                } catch (Exception e) {
                    log.warn("Prompt enhancement failed, using original", e);
                }
            }

            Message assistantMsg = textGenerationService.generateResponse(request, conversation);
            assistantMsg.setConversation(conversation);
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
            return ResponseEntity.badRequest().body(Map.of("success", false, "error", e.getMessage()));
        }
    }

    @PostMapping("/api/chat/new")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> newConversationApi(Principal principal) {
        Long userId = getUserId(principal);
        Conversation conv = newConversation(userId);
        storageService.saveConversation(conv);
        return ResponseEntity.ok(Map.of("conversationId", conv.getId()));
    }

    @GetMapping("/api/chat/conversation/{id}")
    @ResponseBody
    public ResponseEntity<Conversation> getConversation(@PathVariable String id, Principal principal) {
        Long userId = getUserId(principal);
        return storageService.findConversationByUser(id, userId)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @DeleteMapping("/api/chat/conversation/{id}")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> deleteConversation(@PathVariable String id, Principal principal) {
        Long userId = getUserId(principal);
        storageService.deleteConversationByUser(id, userId);
        return ResponseEntity.ok(Map.of("success", true));
    }

    @GetMapping("/api/chat/conversations")
    @ResponseBody
    public ResponseEntity<List<Conversation>> listConversations(
            @RequestParam(required = false) String search, Principal principal) {
        Long userId = getUserId(principal);
        List<Conversation> list = (search != null && !search.isEmpty())
                ? storageService.searchConversationsByUser(search, userId)
                : storageService.getConversationsByUser(userId);
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
    public ResponseEntity<Map<String, Object>> togglePin(@PathVariable String id, Principal principal) {
        Long userId = getUserId(principal);
        storageService.findConversationByUser(id, userId).ifPresent(c -> {
            c.setPinned(!c.isPinned());
            storageService.saveConversation(c);
        });
        return ResponseEntity.ok(Map.of("success", true));
    }

    @PatchMapping("/api/chat/conversation/{id}/model")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> changeModel(@PathVariable String id,
                                                            @RequestBody Map<String, String> body,
                                                            Principal principal) {
        Long userId = getUserId(principal);
        storageService.findConversationByUser(id, userId).ifPresent(c -> {
            c.setModelId(body.get("modelId"));
            storageService.saveConversation(c);
        });
        return ResponseEntity.ok(Map.of("success", true));
    }

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

    private Conversation newConversation(Long userId) {
        return Conversation.builder()
                .title("New Conversation")
                .modelId("us.anthropic.claude-sonnet-4-6")
                .userId(userId)
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

