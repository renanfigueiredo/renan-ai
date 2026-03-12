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

        HttpServletRequest req = (HttpServletRequest) request;
        HttpServletResponse res = (HttpServletResponse) response;

        String proto = req.getHeader("x-forwarded-proto");

        if (proto != null && proto.equals("http")) {
            res.sendRedirect("https://" + req.getServerName() + req.getRequestURI());
            return;
        }

        chain.doFilter(request, response);
    }
}