package com.aimaster.config;

import jakarta.servlet.*;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.Set;

@Component
public class WwwRedirectFilter implements Filter {

    private static final Set<String> REDIRECT_HOSTS = Set.of("evj.app.br", "renan-ai.com.br");

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {

        if (request instanceof HttpServletRequest req
                && response instanceof HttpServletResponse res
                && REDIRECT_HOSTS.contains(req.getHeader("host"))) {
            String redirect = "https://www.evj.app.br" + req.getRequestURI();
            String qs = req.getQueryString();
            if (qs != null) redirect += "?" + qs;
            res.sendRedirect(redirect);
            return;
        }

        chain.doFilter(request, response);
    }
}