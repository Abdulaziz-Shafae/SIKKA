package com.example.sikka.Config;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.web.filter.OncePerRequestFilter;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Base64;

/** Cover HTTP Basic API clients as well as the browser form login. */
@RequiredArgsConstructor
public class BasicAttemptFilter extends OncePerRequestFilter {
    private final AuthenticationAttemptLimiter limiter;

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        String header = request.getHeader("Authorization");
        if (header == null || !header.regionMatches(true, 0, "Basic ", 0, 6)) {
            chain.doFilter(request, response);
            return;
        }
        String username;
        try {
            String value = header.substring(6).trim();
            if (value.length() > 2048) { response.sendError(400); return; }
            String credentials = new String(Base64.getDecoder().decode(value), StandardCharsets.UTF_8);
            int colon = credentials.indexOf(':');
            if (colon <= 0) { response.sendError(400); return; }
            username = credentials.substring(0, colon);
        } catch (IllegalArgumentException ex) {
            response.sendError(400);
            return;
        }
        if (!limiter.allowed("basic-login", username)) {
            response.setStatus(429);
            response.setContentType("application/json;charset=UTF-8");
            response.getWriter().write("{\"message\":\"Too many login attempts. Try again later.\"}");
            return;
        }
        chain.doFilter(request, response);
        if (response.getStatus() == 401) limiter.failed("basic-login", username);
        else limiter.succeeded("basic-login", username);
    }
}
