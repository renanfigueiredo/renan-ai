package com.aimaster.controller;

import com.aimaster.model.*;
import com.aimaster.service.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.security.Principal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.*;

@Slf4j
@Controller
@RequiredArgsConstructor
public class ChatController {

    private final TextGenerationService textGenerationService;
    private final ModelCatalogService modelCatalog;
    private final StorageService storageService;
    private final UserService userService;
    private final PortalContentService portalContentService;

    /**
     * Conjunto rotativo de prompts sugeridos. O conjunto exibido a cada dia é
     * determinístico (mesma data → mesmas sugestões), o que evita "ruído" para
     * o usuário e cria sensação de "card do dia".
     */
    private static final List<List<String>> SUGGESTION_BUCKETS = List.of(
            List.of(
                "Faça um devocional curto sobre Salmos 23",
                "Prepare um esboço de sermão sobre Romanos 8.28",
                "Explique a doutrina da justificação pela fé",
                "Quero estudar 1 João — por onde começo?"
            ),
            List.of(
                "Como conduzir o culto doméstico em família?",
                "Faça uma exegese curta de João 3.16",
                "Diferença entre arrependimento e remorso",
                "Sugira um plano de leitura bíblica em 90 dias"
            ),
            List.of(
                "Como pregar Cristo no Antigo Testamento?",
                "Resuma a Confissão de Fé de Westminster em 5 pontos",
                "O que é a doutrina da expiação penal substitutiva?",
                "Estou desanimado — me lembre do evangelho"
            ),
            List.of(
                "Faça um plano de estudo sobre a Reforma Protestante",
                "Como combater a teologia da prosperidade biblicamente?",
                "Prepare uma EBD para jovens sobre santidade",
                "Como aconselhar alguém em crise de fé?"
            ),
            List.of(
                "Estudo expositivo de Efésios 2.1-10",
                "Como crescer em oração? Princípios bíblicos",
                "Diferença entre lei e evangelho",
                "Sugira versículos para memorizar esta semana"
            ),
            List.of(
                "Prepare um devocional sobre o Salmo 1",
                "O que é a soberania de Deus na salvação?",
                "Como ensinar a Bíblia para crianças?",
                "Esboço de sermão sobre Filipenses 4.4-7"
            ),
            List.of(
                "Estudo sobre os 5 Solas da Reforma",
                "Como tratar a ansiedade segundo a Escritura?",
                "Faça um sermão evangelístico curto",
                "O que significa 'seguir a Cristo'?"
            )
    );

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
                .map(ModelInfo::name).orElse("Claude Sonnet 4.6"));
        uiModel.addAttribute("conversations", storageService.getConversationsByUser(userId));
        return "chat";
    }

    /**
     * Legacy endpoint — cached browsers may still call this.
     * Delegates to the streaming endpoint so it won't H12 on Heroku.
     */
    @PostMapping(value = "/api/chat/send", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    @ResponseBody
    public SseEmitter sendMessage(@RequestBody ChatRequest request, Principal principal) {
        return streamMessage(request, principal);
    }

    /**
     * Streaming endpoint — returns SSE events so the browser renders tokens as they arrive.
     * This avoids Heroku's 30-second H12 timeout for long LLM responses.
     */
    @PostMapping(value = "/api/chat/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    @ResponseBody
    public SseEmitter streamMessage(@RequestBody ChatRequest request, Principal principal) {
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

            // Add user message to conversation
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

            if (Boolean.TRUE.equals(request.getEnhancePrompt())) {
                try {
                    request.setMessage(textGenerationService.enhancePrompt(request.getMessage(), "chat"));
                } catch (Exception e) {
                    log.warn("Prompt enhancement failed, using original", e);
                }
            }

            // Persist user message immediately so it's saved even if streaming is interrupted
            storageService.saveConversation(conversation);

            return textGenerationService.streamResponse(request, conversation);

        } catch (Exception e) {
            log.error("Stream setup error", e);
            SseEmitter errorEmitter = new SseEmitter(0L);
            try {
                errorEmitter.send(SseEmitter.event()
                        .data("{\"type\":\"error\",\"message\":\"" + e.getMessage() + "\"}"));
            } catch (Exception _) {}
            errorEmitter.complete();
            return errorEmitter;
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

    /**
     * Gera 3 perguntas curtas de follow-up baseadas na última troca da conversa.
     * Retorna {"followups": ["...", "...", "..."]}. Lista vazia se não conseguir gerar.
     * Endpoint best-effort — frontend não deve falhar se vier vazio.
     */
    @PostMapping("/api/chat/followups")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> followups(@RequestBody Map<String, String> body) {
        String userMsg = body.getOrDefault("userMessage", "");
        String assistantReply = body.getOrDefault("assistantReply", "");
        List<String> suggestions = textGenerationService.generateFollowups(userMsg, assistantReply);
        return ResponseEntity.ok(Map.of("followups", suggestions, "success", true));
    }

    /**
     * Retorna o "card do dia" da home do chat: versículo do dia + 4 sugestões
     * de prompts rotativas. Determinístico por dia — mesma data devolve sempre
     * o mesmo card, criando hábito devocional estável.
     */
    @GetMapping("/api/chat/daily")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> daily() {
        Map<String, Object> response = new LinkedHashMap<>();

        Map.Entry<String, String> verse = portalContentService.getVerseOfTheDay();
        if (verse != null) {
            response.put("verse", Map.of(
                    "reference", verse.getKey(),
                    "text", verse.getValue()
            ));
        }

        // Bucket rotativo determinístico (dia do ano % buckets.size)
        LocalDate today = LocalDate.now(ZoneId.of("America/Sao_Paulo"));
        int idx = Math.floorMod(today.getYear() * 1000 + today.getDayOfYear(),
                                SUGGESTION_BUCKETS.size());
        response.put("suggestions", SUGGESTION_BUCKETS.get(idx));
        response.put("date", today.toString());

        return ResponseEntity.ok(response);
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

