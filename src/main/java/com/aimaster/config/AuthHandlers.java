package com.aimaster.config;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.security.web.authentication.SimpleUrlAuthenticationSuccessHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Set;

/**
 * Handlers de autenticação que melhoram a UX de sessão expirada:
 *
 * <ul>
 *   <li>{@link SmartAuthenticationEntryPoint}: para chamadas AJAX/SSE (fetch,
 *       SSE streaming, etc.) responde com {@code 401} + JSON em vez de redirect
 *       HTML, para o frontend detectar e redirecionar com {@code ?next=}.
 *       Para navegação normal (form submit, link), mantém o redirect para
 *       {@code /login?next=<path>}.</li>
 *   <li>{@link NextAwareSuccessHandler}: depois do login, se houver um
 *       parâmetro {@code next} no formulário, redireciona para esse path
 *       (validado contra open-redirect) em vez do {@code /dashboard} fixo.</li>
 * </ul>
 */
public final class AuthHandlers {

    private AuthHandlers() {}

    /** Cabeçalhos / paths que indicam chamada XHR/SSE (não navegação). */
    private static final Set<String> AJAX_API_PREFIXES = Set.of(
            "/api/", "/ws/", "/guest/heartbeat", "/guest/status"
    );

    /** Detecta se a requisição é um fetch/SSE ou navegação tradicional. */
    static boolean isAjaxOrApi(HttpServletRequest request) {
        String accept = request.getHeader(HttpHeaders.ACCEPT);
        if (accept != null) {
            // SSE
            if (accept.contains(MediaType.TEXT_EVENT_STREAM_VALUE)) return true;
            // fetch JSON tipicamente envia Accept: application/json (sem text/html)
            if (accept.contains(MediaType.APPLICATION_JSON_VALUE) && !accept.contains(MediaType.TEXT_HTML_VALUE)) {
                return true;
            }
        }
        String requestedWith = request.getHeader("X-Requested-With");
        if (requestedWith != null && requestedWith.equalsIgnoreCase("XMLHttpRequest")) return true;

        String path = request.getRequestURI();
        for (String prefix : AJAX_API_PREFIXES) {
            if (path.startsWith(prefix)) return true;
        }
        return false;
    }

    /**
     * Devolve um path seguro (mesma origem, começando com /) ou null.
     * Bloqueia open-redirect: se vier "//evil.com" ou "http://...", retorna null.
     */
    static String sanitizeNext(String next) {
        if (next == null || next.isBlank()) return null;
        if (!next.startsWith("/")) return null;          // só paths relativos
        if (next.startsWith("//")) return null;          // protocol-relative URL = perigo
        if (next.startsWith("/login")) return null;      // evita loop
        if (next.startsWith("/logout")) return null;
        if (next.length() > 512) return null;            // sanidade
        // Garante que é um URI válido (sem chars de controle)
        try {
            new URI(next);
        } catch (URISyntaxException e) {
            return null;
        }
        return next;
    }

    /**
     * Constrói /login?next=<encoded> para preservar a URL alvo durante login.
     */
    static String buildLoginRedirect(HttpServletRequest request) {
        String original = request.getRequestURI();
        String qs = request.getQueryString();
        String full = (qs == null || qs.isBlank()) ? original : original + "?" + qs;
        String safe = sanitizeNext(full);
        if (safe == null) return "/login";
        return "/login?next=" + URLEncoder.encode(safe, StandardCharsets.UTF_8);
    }

    /** EntryPoint que distingue navegação (302 → /login?next=) de XHR/SSE (401 JSON). */
    @Component
    public static class SmartAuthenticationEntryPoint implements AuthenticationEntryPoint {
        @Override
        public void commence(HttpServletRequest request,
                             HttpServletResponse response,
                             org.springframework.security.core.AuthenticationException authException)
                throws IOException {
            if (isAjaxOrApi(request)) {
                response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                response.setContentType(MediaType.APPLICATION_JSON_VALUE);
                response.setCharacterEncoding(StandardCharsets.UTF_8.name());
                response.setHeader("X-Auth-Required", "1");
                response.getWriter().write(
                        "{\"status\":401,\"error\":\"UNAUTHORIZED\",\"message\":\"Sessão expirada. Faça login novamente.\"}");
                return;
            }
            response.sendRedirect(buildLoginRedirect(request));
        }
    }

    /** Após login bem-sucedido, redireciona para o ?next= sanitizado, se houver. */
    @Component
    public static class NextAwareSuccessHandler extends SimpleUrlAuthenticationSuccessHandler {
        public NextAwareSuccessHandler() {
            setDefaultTargetUrl("/dashboard");
            setAlwaysUseDefaultTargetUrl(false);
        }

        @Override
        protected String determineTargetUrl(HttpServletRequest request, HttpServletResponse response) {
            String next = sanitizeNext(request.getParameter("next"));
            if (next != null) return next;
            return super.determineTargetUrl(request, response);
        }
    }
}
