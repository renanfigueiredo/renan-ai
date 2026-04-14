package com.aimaster.config;

import jakarta.servlet.*;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.stereotype.Component;

import java.io.IOException;

@Component
public class HttpsRedirectFilter implements Filter {

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {

        if (request instanceof HttpServletRequest req
                && response instanceof HttpServletResponse res
                && "http".equals(req.getHeader("x-forwarded-proto"))) {
            String redirect = "https://" + req.getServerName() + req.getRequestURI();
            String qs = req.getQueryString();
            if (qs != null) redirect += "?" + qs;
            res.sendRedirect(redirect);
            return;
        }

        chain.doFilter(request, response);
    }
}