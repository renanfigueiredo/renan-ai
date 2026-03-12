package com.aimaster.config;

import jakarta.servlet.*;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.stereotype.Component;

import java.io.IOException;

@Component
public class WwwRedirectFilter implements Filter {

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {

        HttpServletRequest req = (HttpServletRequest) request;
        HttpServletResponse res = (HttpServletResponse) response;

        String host = req.getHeader("host");

        if ("renan-ai.com.br".equals(host)) {
            res.sendRedirect("https://www.renan-ai.com.br" + req.getRequestURI());
            return;
        }

        chain.doFilter(request, response);
    }
}