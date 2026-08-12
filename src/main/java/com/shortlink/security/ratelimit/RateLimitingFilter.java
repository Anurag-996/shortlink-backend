package com.shortlink.security.ratelimit;

import com.shortlink.exception.ErrorResponse;
import com.shortlink.security.ratelimit.RateLimiterConfig.TokenBucketLimit;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import tools.jackson.databind.json.JsonMapper;

import java.io.IOException;
import java.time.LocalDateTime;

// Global route-aware rate limiting filter backed by Redis Token Bucket algorithm.
@Slf4j
@Component
@RequiredArgsConstructor
public class RateLimitingFilter extends OncePerRequestFilter {

    private final RedisRateLimiter redisRateLimiter;
    private final ClientKeyResolver clientKeyResolver;
    private final JsonMapper jsonMapper;

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain) throws ServletException, IOException {

        // Early bypass for CORS preflight OPTIONS requests
        if ("OPTIONS".equalsIgnoreCase(request.getMethod())) {
            filterChain.doFilter(request, response);
            return;
        }

        String path = request.getRequestURI();
        String method = request.getMethod();

        // 1. Whitelist actuator / health check endpoints
        if (!RateLimiterConfig.ENABLED || path.startsWith("/actuator")) {
            filterChain.doFilter(request, response);
            return;
        }

        // 2. Identify route category and corresponding configured Token Bucket limit
        RouteProfile profile = resolveRouteProfile(method, path);

        // 3. Identify client key (verified user ID from JWT if available, else client IP)
        String clientKey = clientKeyResolver.resolveKey(request, RateLimiterConfig.TRUST_PROXY);

        // 4. Evaluate Token Bucket rate limit atomically in Redis
        RedisRateLimiter.RateLimitResult result = redisRateLimiter.isAllowed(
                profile.category(),
                clientKey,
                profile.limit().capacity(),
                profile.limit().refillTokensPerSecond()
        );

        // 5. Inject rate limit diagnostic headers (capacity and remaining tokens)
        response.setHeader("X-RateLimit-Limit", String.valueOf(result.limit()));
        response.setHeader("X-RateLimit-Remaining", String.valueOf(result.remaining()));

        // 6. Handle rate limit rejection
        if (!result.allowed()) {
            response.setStatus(HttpStatus.TOO_MANY_REQUESTS.value());
            response.setContentType(MediaType.APPLICATION_JSON_VALUE);
            response.setHeader("Retry-After", String.valueOf(result.retryAfterSeconds()));

            ErrorResponse errorPayload = ErrorResponse.builder()
                    .success(false)
                    .status(HttpStatus.TOO_MANY_REQUESTS.value())
                    .error(HttpStatus.TOO_MANY_REQUESTS.getReasonPhrase())
                    .message("Rate limit exceeded. Please try again in " + result.retryAfterSeconds() + " seconds.")
                    .path(path)
                    .timestamp(LocalDateTime.now())
                    .build();

            jsonMapper.writeValue(response.getOutputStream(), errorPayload);
            log.warn("Rate limit exceeded: category=[{}], capacity=[{}]", profile.category(), result.limit());
            return;
        }

        filterChain.doFilter(request, response);
    }

    private RouteProfile resolveRouteProfile(String method, String path) {
        if ("POST".equalsIgnoreCase(method)) {
            if (path.equals("/api/auth/login")) {
                return new RouteProfile("auth_login", RateLimiterConfig.AUTH_LOGIN);
            }
            if (path.equals("/api/auth/refresh")) {
                return new RouteProfile("auth_refresh", RateLimiterConfig.AUTH_REFRESH);
            }
            if (path.equals("/api/auth/logout") || path.equals("/api/auth/logout-all")) {
                return new RouteProfile("auth_logout", RateLimiterConfig.AUTH_LOGOUT);
            }
            if (path.startsWith("/api/v1/urls") || path.startsWith("/api/urls")) {
                return new RouteProfile("url_creation", RateLimiterConfig.URL_CREATION);
            }
        }

        if (path.startsWith("/api/analytics")) {
            return new RouteProfile("analytics", RateLimiterConfig.ANALYTICS);
        }

        // Short URL redirect endpoint (e.g. GET /{shortCode})
        if ("GET".equalsIgnoreCase(method) && !path.startsWith("/api/")) {
            return new RouteProfile("url_redirect", RateLimiterConfig.URL_REDIRECT);
        }

        return new RouteProfile("default", RateLimiterConfig.DEFAULT_LIMIT);
    }

    private record RouteProfile(String category, TokenBucketLimit limit) {}
}
