package com.shortlink.security.ratelimit;

import com.shortlink.security.jwt.JwtService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Component;

// Resolves a unique client identifier for rate limiting based on verified JWT identity or client IP.
@Slf4j
@Component
@RequiredArgsConstructor
public class ClientKeyResolver {

    private final JwtService jwtService;

    // Resolves client key from verified JWT subject if authentic, otherwise falls back to client IP.
    public String resolveKey(HttpServletRequest request, boolean trustProxy) {
        String authHeader = request.getHeader(HttpHeaders.AUTHORIZATION);

        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            String token = authHeader.substring(7).trim();
            if (jwtService.isTokenValid(token)) {
                String subject = jwtService.extractUsername(token);
                if (subject != null && !subject.isBlank()) {
                    return "user:" + sanitizeKey(subject);
                }
            }
        }

        return "ip:" + extractClientIp(request, trustProxy);
    }

    private String extractClientIp(HttpServletRequest request, boolean trustProxy) {
        if (trustProxy) {
            String xForwarded = request.getHeader("X-Forwarded-For");
            if (xForwarded != null && !xForwarded.isBlank()) {
                return xForwarded.split(",")[0].trim();
            }
        }
        return request.getRemoteAddr();
    }

    private String sanitizeKey(String key) {
        return key.replaceAll("[^a-zA-Z0-9_.-]", "_");
    }
}
