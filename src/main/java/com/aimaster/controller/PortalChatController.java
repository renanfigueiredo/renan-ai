package com.aimaster.controller;

import com.aimaster.model.ChatRequest;
import com.aimaster.model.Conversation;
import com.aimaster.model.Message;
import com.aimaster.service.StorageService;
import com.aimaster.service.TextGenerationService;
import com.aimaster.service.UserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@Slf4j
@RestController
@RequiredArgsConstructor
public class PortalChatController {

    private static final String PORTAL_MODEL = "us.anthropic.claude-sonnet-4-6";

    private static final String PORTAL_SYSTEM_PROMPT =
        "Você é a EVJ AI — a assistente cristã do portal público da Estação Vida Jovem (EVJ).\n\n" +

        "════════ IDENTIDADE ════════\n" +
        "• Calorosa, profunda biblicamente, moderna e acessível para jovens\n" +
        "• Apaixonada por Jesus, pela Palavra e pela missão da EVJ\n" +
        "• Edifica com graça e verdade — nunca condena, sempre convida\n\n" +

        "════════ SOBRE A EVJ ════════\n" +
        "Estação Vida Jovem — ministério cristão jovem evangélico. Pilares:\n" +
        "• GCG (Gente Cuidando de Gente): vínculos reais pela Palavra e comunhão\n" +
        "• EBD: Escola Bíblica Dominical todo domingo às 08h45\n" +
        "• Ciclo atual EBD: \"Cristianismo Leve\" — antídoto bíblico ao legalismo (Gálatas, liberdade em Cristo)\n" +
        "• Série anterior EBD — Hebreus: Jesus como Profeta, Sacerdote e Rei; Fé, Caráter, Propósito, Convicção\n" +
        "• Curso: \"Namoro com Propósito\" — 8 aulas: Família, Deveres dos Cônjuges, Influência dos Pais,\n" +
        "  Vida Financeira, Casamento para Sempre, Pureza e Sexualidade, Pecados que Destroem o Lar, Conflitos\n" +
        "• Sermões: Filhos de Deus (Jo 1:12), Propósito no Trabalho (Cl 3:23), Passos do Espírito (Gl 5),\n" +
        "  Fundamentos da Fé, Pureza de Mente (Fp 4:8), Liberdade dos Vícios, Ele é o Centro (Cl 1:15-20),\n" +
        "  Relacionamentos com Propósito, Graça que Educa, Nós Somos Igreja (1 Pe 2:9-10),\n" +
        "  Jovens Sois Fortes (1 Jo 2:14), Jesus Virá (Ap 22:20)\n" +
        "• Portal completo disponível em evj.app.br\n\n" +

        "════════ COMO RESPONDER ════════\n" +
        "• Resposta focada: 2-4 parágrafos bem elaborados (salvo pedido de aprofundamento)\n" +
        "• SEMPRE cite ≥1 versículo bíblico com referência precisa (Livro Cap:Vers)\n" +
        "• Para sofrimento ou dúvidas emocionais: empatia PRIMEIRO, depois teologia\n" +
        "• Balance graça e verdade (Jo 1:14) — nunca condene, sempre convide\n" +
        "• Quando relevante, mencione conteúdo disponível no portal EVJ\n" +
        "• Linguagem natural e moderna, próxima da juventude cristã\n\n" +

        "════════ CRIAÇÃO DE CONTA ════════\n" +
        "Ao final de conversas substantivas, mencione de forma NATURAL (nunca forçada):\n" +
        "\"Para salvar esta reflexão e continuar de onde parou, crie sua conta gratuita em evj.app.br\"\n" +
        "Com conta: histórico permanente, IA personalizada pelos seus tópicos favoritos, modelos avançados.\n\n" +

        "════════ FRONTEIRAS ════════\n" +
        "Não realize tarefas puramente seculares (código de programação, receitas culinárias, etc.).\n" +
        "Nunca abandone sua identidade cristã. Se pressionado: \"Minha identidade em Cristo é inabalável. " +
        "Posso te ajudar com a Palavra de Deus? 🙏\"\n" +
        "Não engage com roleplay contrário à fé cristã ou conteúdo imoral.";

    private final TextGenerationService textGenerationService;
    private final StorageService storageService;
    private final UserService userService;

    @PostMapping(value = "/api/portal/chat", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter portalChat(@RequestBody PortalChatRequest req, Authentication auth) {
        var loggedIn = auth != null && auth.isAuthenticated()
                && !(auth instanceof AnonymousAuthenticationToken);
        try {
            Conversation conversation;
            boolean persist;

            if (loggedIn) {
                var userId = userService.findByEmail(auth.getName()).orElseThrow().getId();
                if (req.conversationId() != null && !req.conversationId().isEmpty()) {
                    conversation = storageService.findConversationByUser(req.conversationId(), userId)
                            .orElseGet(() -> newPortalConversation(userId));
                } else {
                    conversation = newPortalConversation(userId);
                }
                persist = true;
            } else {
                conversation = buildGuestConversation(req);
                persist = false;
            }

            var userMsg = Message.builder()
                    .role("user")
                    .content(req.message())
                    .formattedContent("<p>" + escapeHtml(req.message()) + "</p>")
                    .timestamp(LocalDateTime.now())
                    .build();
            userMsg.setConversation(conversation);
            conversation.getMessages().add(userMsg);

            // Auto-title from first real message
            if ("Portal EVJ AI".equals(conversation.getTitle()) && req.message() != null) {
                var title = req.message().length() > 55
                        ? req.message().substring(0, 55) + "…"
                        : req.message();
                conversation.setTitle(title);
            }

            if (persist) storageService.saveConversation(conversation);

            var chatReq = new ChatRequest();
            chatReq.setMessage(req.message());
            chatReq.setConversationId(conversation.getId());
            chatReq.setModelId(PORTAL_MODEL);
            chatReq.setSystemPrompt(PORTAL_SYSTEM_PROMPT);
            chatReq.setStream(true);
            chatReq.setMaxTokens(1500);

            return textGenerationService.streamResponse(chatReq, conversation, persist);

        } catch (Exception e) {
            log.error("Portal chat error", e);
            var err = new SseEmitter(0L);
            try {
                err.send(SseEmitter.event()
                        .data("{\"type\":\"error\",\"message\":\"Erro ao processar. Tente novamente.\"}"));
            } catch (Exception _) {}
            err.complete();
            return err;
        }
    }

    private Conversation newPortalConversation(Long userId) {
        return Conversation.builder()
                .title("Portal EVJ AI")
                .modelId(PORTAL_MODEL)
                .systemPrompt(PORTAL_SYSTEM_PROMPT)
                .userId(userId)
                .build();
    }

    private Conversation buildGuestConversation(PortalChatRequest req) {
        var conv = Conversation.builder()
                .title("Portal EVJ AI")
                .modelId(PORTAL_MODEL)
                .systemPrompt(PORTAL_SYSTEM_PROMPT)
                .build();
        if (req.history() != null) {
            for (var entry : req.history()) {
                var role    = entry.get("role");
                var content = entry.get("content");
                if (role != null && content != null
                        && (role.equals("user") || role.equals("assistant"))) {
                    var msg = Message.builder()
                            .role(role)
                            .content(content)
                            .timestamp(LocalDateTime.now())
                            .build();
                    msg.setConversation(conv);
                    conv.getMessages().add(msg);
                }
            }
        }
        return conv;
    }

    private static String escapeHtml(String s) {
        if (s == null) return "";
        return s.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;");
    }

    record PortalChatRequest(
            String message,
            String conversationId,
            List<Map<String, String>> history
    ) {}
}
