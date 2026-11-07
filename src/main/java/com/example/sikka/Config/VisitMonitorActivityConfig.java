package com.example.sikka.Config;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.servlet.HandlerInterceptor;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * Browser/API activity arming without any periodic database query.
 * Reconcile active visits after successful start/finish requests.
 */
@Configuration
@RequiredArgsConstructor
public class VisitMonitorActivityConfig implements WebMvcConfigurer {
    private final VisitMonitorGate gate;

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(new HandlerInterceptor() {
            @Override
            public void afterCompletion(HttpServletRequest request, HttpServletResponse response,
                                        Object handler, Exception error) {
                if (error != null || response.getStatus() >= 400) return;
                String path = request.getServletPath();
                // Public park exploration also needs fresh ride data while a browser is using SIKKA.
                if ("GET".equals(request.getMethod()) &&
                    (path.startsWith("/api/v1/ride/") || path.startsWith("/api/v1/map/")))
                    gate.recordVisitRequest();
                Authentication auth = SecurityContextHolder.getContext().getAuthentication();
                if (auth == null || !auth.isAuthenticated() || "anonymousUser".equals(auth.getPrincipal())) return;
                if (path.startsWith("/api/v1/visit/") ||
                    path.startsWith("/api/v1/visit-plan/") ||
                    path.startsWith("/api/v1/activity-session/") ||
                    path.startsWith("/api/v1/visit-record/")) gate.recordVisitRequest();

                // Commit finished before returning an HTTP 2xx response.
                if ("PUT".equals(request.getMethod()) &&
                    (path.matches("^/api/v1/visit/start/[0-9]+$") ||
                     path.matches("^/api/v1/visit/complete/[0-9]+$"))) {
                    gate.refreshActiveVisits();
                }
            }
        });
    }
}
