package com.aimaster.config;

import jakarta.servlet.Filter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.io.IOException;

/**
 * Adiciona headers padrão de segurança em todas as respostas HTTP:
 * - X-Frame-Options: protege contra clickjacking
 * - X-Content-Type-Options: bloqueia MIME-sniffing
 * - Referrer-Policy: limita o que é enviado em Referer
 * - Permissions-Policy: desativa APIs sensíveis (câmera, microfone, geo)
 * - Content-Security-Policy: política básica permitindo CDNs usados (Bootstrap, Google Fonts)
 *
 * O HSTS é tratado em {@link HstsFilter}; aqui não duplicamos.
 */
@Component
@Order(Ordered.HIGHEST_PRECEDENCE + 20)
public class SecurityHeadersFilter implements Filter {

    private static final String CSP =
            "default-src 'self'; "
            + "script-src 'self' 'unsafe-inline' https://cdn.jsdelivr.net https://cdnjs.cloudflare.com; "
            + "style-src 'self' 'unsafe-inline' https://cdn.jsdelivr.net https://fonts.googleapis.com; "
            + "font-src 'self' https://fonts.gstatic.com https://cdn.jsdelivr.net data:; "
            + "img-src 'self' data: https: blob:; "
            + "media-src 'self' blob:; "
            + "connect-src 'self' https: wss:; "
            + "frame-ancestors 'none'; "
            + "base-uri 'self'; "
            + "form-action 'self'";

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {

        if (response instanceof HttpServletResponse res) {
            res.setHeader("X-Frame-Options", "DENY");
            res.setHeader("X-Content-Type-Options", "nosniff");
            res.setHeader("Referrer-Policy", "strict-origin-when-cross-origin");
            res.setHeader("Permissions-Policy",
                    "camera=(), microphone=(), geolocation=(), payment=(), usb=()");
            if (!res.containsHeader("Content-Security-Policy")) {
                res.setHeader("Content-Security-Policy", CSP);
            }
        }

        chain.doFilter(request, response);
    }
}
