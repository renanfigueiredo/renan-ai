package com.aimaster.controller;

import com.aimaster.model.GuestSession;
import com.aimaster.service.GuestSessionService;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/guest")
@RequiredArgsConstructor
public class GuestController {

    private final GuestSessionService guestSessionService;

    /**
     * Inicia ou retoma uma sessão visitante.
     * Recebe o fingerprint do browser calculado no cliente.
     */
    @PostMapping("/start")
    public ResponseEntity<Map<String, Object>> start(
            @RequestBody Map<String, String> body,
            HttpServletRequest request,
            HttpServletResponse response) {

        String fingerprint = body.get("fingerprint");
        if (fingerprint == null || fingerprint.isBlank() || fingerprint.length() != 64) {
            return ResponseEntity.badRequest().body(Map.of("error", "fingerprint_invalid"));
        }

        String ip = extractIp(request);

        try {
            GuestSession session = guestSessionService.createOrResumeSession(fingerprint, ip);

            // Cookie HttpOnly — JavaScript não consegue ler nem apagar
            Cookie cookie = new Cookie("GUEST_TOKEN", session.getId());
            cookie.setHttpOnly(true);
            cookie.setSecure(request.isSecure());
            cookie.setPath("/");
            cookie.setMaxAge(7 * 24 * 3600); // cookie dura 7 dias; tempo real rastreado no DB
            response.addCookie(cookie);

            return ResponseEntity.ok(Map.of(
                    "ok", true,
                    "remainingSeconds", session.getRemainingSeconds()
            ));

        } catch (IllegalStateException e) {
            String code = e.getMessage();
            String msg = switch (code) {
                case "guest.expired"      -> "Seu período de visita expirou. Crie uma conta para continuar.";
                case "guest.rate_limited" -> "Limite de sessões visitante atingido para este dispositivo.";
                default                   -> "Não foi possível iniciar a sessão visitante.";
            };
            return ResponseEntity.status(403).body(Map.of("error", code, "message", msg));
        }
    }

    /**
     * Heartbeat: cliente informa quantos segundos ficou visível desde o último envio.
     * Chamado a cada ~25 s enquanto a aba está ativa.
     */
    @PostMapping("/heartbeat")
    public ResponseEntity<Map<String, Object>> heartbeat(
            @RequestBody Map<String, Integer> body,
            @CookieValue(name = "GUEST_TOKEN", required = false) String token) {

        if (token == null) {
            return ResponseEntity.ok(sessionMap(true, 0));
        }

        int delta = body.getOrDefault("delta", 0);
        GuestSession session = guestSessionService.heartbeat(token, delta);

        return ResponseEntity.ok(sessionMap(session.isExpired(), session.getRemainingSeconds()));
    }

    /**
     * Retorna o estado atual da sessão visitante (usado no carregamento da página).
     */
    @GetMapping("/status")
    public ResponseEntity<Map<String, Object>> status(
            @CookieValue(name = "GUEST_TOKEN", required = false) String token) {

        if (token == null) {
            return ResponseEntity.ok(sessionMap(true, 0));
        }

        return guestSessionService.findById(token)
                .map(s -> ResponseEntity.ok(sessionMap(s.isExpired(), s.getRemainingSeconds())))
                .orElseGet(() -> ResponseEntity.ok(sessionMap(true, 0)));
    }

    // ── Util ──────────────────────────────────────────────────────────────────

    private static Map<String, Object> sessionMap(boolean expired, int remaining) {
        return Map.of("expired", expired, "remainingSeconds", remaining);
    }

    private String extractIp(HttpServletRequest request) {
        var xff = request.getHeader("X-Forwarded-For");
        if (xff != null && !xff.isBlank()) return xff.split(",")[0].trim();
        return request.getRemoteAddr();
    }
}
