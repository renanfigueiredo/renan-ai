package com.aimaster.config;

import com.aimaster.repository.UserRepository;
import com.aimaster.service.GuestSessionService;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;
import java.util.Set;

/**
 * Lê o cookie GUEST_TOKEN, valida a sessão visitante no banco e, se válida,
 * injeta um Authentication com ROLE_GUEST no SecurityContext.
 * Isso permite que o visitante acesse as páginas protegidas sem criar uma
 * conta real, enquanto o tempo é rastreado server-side.
 */
@Component
@RequiredArgsConstructor
public class GuestAuthFilter extends OncePerRequestFilter {

    private final GuestSessionService guestSessionService;
    private final UserRepository userRepository;

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain chain) throws ServletException, IOException {

        // Só processamos se não há autenticação real já definida
        Authentication existing = SecurityContextHolder.getContext().getAuthentication();
        boolean alreadyAuth = existing != null
                && existing.isAuthenticated()
                && !(existing instanceof AnonymousAuthenticationToken);

        if (!alreadyAuth) {
            var cookies = request.getCookies();
            if (cookies != null) {
                for (var c : cookies) {
                    if ("GUEST_TOKEN".equals(c.getName())) {
                        processGuestCookie(c.getValue());
                        break;
                    }
                }
            }
        }

        chain.doFilter(request, response);
    }

    private void processGuestCookie(String token) {
        guestSessionService.findValid(token).ifPresent(session ->
                userRepository.findById(session.getAppUserId()).ifPresent(user -> {
                    var auth = new UsernamePasswordAuthenticationToken(
                            user.getEmail(),
                            null,
                            List.of(new SimpleGrantedAuthority("ROLE_GUEST"))
                    );
                    SecurityContextHolder.getContext().setAuthentication(auth);
                })
        );
    }

    private static final Set<String> EXACT_SKIP  = Set.of("/", "/login", "/register", "/pending",
            "/forgot-password", "/reset-password", "/favicon.ico");
    private static final List<String> PREFIX_SKIP = List.of("/portal", "/admin/", "/css/", "/js/",
            "/images/", "/guest/start");

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        var path = request.getServletPath();
        return EXACT_SKIP.contains(path) || PREFIX_SKIP.stream().anyMatch(path::startsWith);
    }
}
