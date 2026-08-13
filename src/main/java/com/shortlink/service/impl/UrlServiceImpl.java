package com.shortlink.service.impl;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import org.springframework.security.access.AccessDeniedException;

import com.shortlink.dto.redis.CachedUrl;
import com.shortlink.dto.request.CreateShortUrlRequest;
import com.shortlink.dto.response.PageResponse;
import com.shortlink.dto.response.ShortUrlResponse;
import com.shortlink.entity.Url;
import com.shortlink.exception.CustomAliasAlreadyExistsException;
import com.shortlink.exception.UrlExpiredException;
import com.shortlink.exception.UrlNotFoundException;
import com.shortlink.repository.UrlRepository;
import com.shortlink.security.util.SecurityUtils;
import com.shortlink.service.RedisCacheService;
import com.shortlink.service.ShortCodeGenerator;
import com.shortlink.service.UrlMapper;
import com.shortlink.service.UrlService;
import com.shortlink.user.Role;
import com.shortlink.user.User;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.util.Set;

import com.shortlink.exception.BadRequestException;

// Implementation of UrlService enforcing clean architecture and expiration-aware Cache-Aside pattern.
@Slf4j
@Service
@RequiredArgsConstructor
public class UrlServiceImpl implements UrlService {

    private static final int MAX_GENERATION_ATTEMPTS = 5;

    private static final Set<String> RESERVED_ALIASES = Set.of(
            "login", "register", "signup", "signin", "logout",
            "app", "api", "admin", "dashboard", "urls", "settings",
            "auth", "user", "users", "analytics", "overview",
            "forgot-password", "reset-password", "verify-email",
            "robots.txt", "sitemap.xml", "favicon.ico", "icon.png",
            "static", "assets", "public", "health", "actuator",
            "docs", "swagger", "swagger-ui", "v3", "error", "404"
    );

    private final UrlRepository urlRepository;
    private final RedisCacheService redisCacheService;
    private final ShortCodeGenerator shortCodeGenerator;
    private final UrlMapper urlMapper;

    @Value("${app.base-url:http://localhost:8080}")
    private String baseUrl;

    @Override
    @Transactional
    public ShortUrlResponse createShortUrl(CreateShortUrlRequest request) {
        String shortCode;

        if (request.customAlias() != null && !request.customAlias().isBlank()) {
            shortCode = request.customAlias().trim();
            if (RESERVED_ALIASES.contains(shortCode.toLowerCase())) {
                throw new BadRequestException("The custom alias '" + shortCode + "' is a reserved keyword and cannot be used");
            }
            if (urlRepository.existsByShortCode(shortCode)) {
                throw new CustomAliasAlreadyExistsException("Custom alias '" + shortCode + "' is already in use");
            }
        } else {
            shortCode = generateUniqueShortCode();
        }

        Url urlEntity = urlMapper.toEntity(request, shortCode);

        User currentUser = SecurityUtils.getCurrentUserOrNull();
        if (currentUser != null) {
            urlEntity.setUser(currentUser);
        }

        Url savedUrl = urlRepository.save(urlEntity);
        log.info("Successfully created short URL with code: {} for user: {}", savedUrl.getShortCode(), currentUser != null ? currentUser.getEmail() : "anonymous");

        // Note: As per architecture rules, URL creation is NOT cached in Redis.
        return urlMapper.toResponse(savedUrl, baseUrl);
    }

    @Override
    @Transactional
    public String getOriginalUrlAndIncrementClick(String shortCode) {
        // Cache-Aside Step 1: Read from Redis
        Optional<CachedUrl> cachedUrlOpt = redisCacheService.getCachedUrl(shortCode);

        if (cachedUrlOpt.isPresent()) {
            CachedUrl cachedUrl = cachedUrlOpt.get();

            // Check if cached entry has expired
            if (cachedUrl.isExpired()) {
                log.warn("Cached short code [{}] has expired; evicting from Redis", shortCode);
                redisCacheService.evictUrl(shortCode);
                throw new UrlExpiredException("Short URL with code '" + shortCode + "' has expired");
            }

            log.debug("Cache hit for shortCode: {}", shortCode);
            // Increment click count in PostgreSQL for a cache hit.
            incrementClickCountSilently(shortCode);
            return cachedUrl.originalUrl();
        }

        log.debug("Cache miss for shortCode: {}", shortCode);

        // Cache-Aside Step 2: Read from PostgreSQL
        Url url = urlRepository.findByShortCode(shortCode)
                .orElseThrow(() -> new UrlNotFoundException("Short URL not found for code: " + shortCode));

        // Expiration check
        if (url.getExpiresAt() != null && url.getExpiresAt().isBefore(LocalDateTime.now())) {
            log.warn("Short code {} has expired at {}", shortCode, url.getExpiresAt());
            throw new UrlExpiredException("Short URL with code '" + shortCode + "' has expired");
        }

        // Cache-Aside Step 3: Store in Redis with dynamic TTL matching URL expiration
        redisCacheService.cacheUrl(shortCode, url.getOriginalUrl(), url.getExpiresAt());

        // Increment click count in PostgreSQL
        urlRepository.incrementClickCount(url.getId());

        return url.getOriginalUrl();
    }

    @Override
    @Transactional(readOnly = true)
    public ShortUrlResponse getUrlByShortCode(String shortCode) {
        Url url = urlRepository.findByShortCode(shortCode)
                .orElseThrow(() -> new UrlNotFoundException("Short URL not found for code: " + shortCode));

        return urlMapper.toResponse(url, baseUrl);
    }

    @Override
    @Transactional(readOnly = true)
    public PageResponse<ShortUrlResponse> getAllUrls(int page, int size, String sortBy, String direction) {
        int validPage = Math.max(0, page);
        int validSize = size > 0 ? Math.min(size, 100) : 10;

        String property = "createdAt";
        if (sortBy != null && !sortBy.isBlank()) {
            String clean = sortBy.trim().toLowerCase();
            if (null != clean) switch (clean) {
                case "createdat", "created" -> property = "createdAt";
                case "expiresat", "expiry", "status" -> property = "expiresAt";
                case "clickcount", "clicks" -> property = "clickCount";
                default -> {
                }
            }
        }

        Sort.Direction sortDirection = "asc".equalsIgnoreCase(direction) ? Sort.Direction.ASC : Sort.Direction.DESC;
        Pageable pageable = PageRequest.of(validPage, validSize, Sort.by(sortDirection, property));

        User currentUser = SecurityUtils.getCurrentUserOrNull();
        Page<Url> urlPage;
        if (currentUser != null && currentUser.getRole() != Role.ADMIN) {
            urlPage = urlRepository.findAllByUser(currentUser, pageable);
        } else {
            urlPage = urlRepository.findAll(pageable);
        }

        List<ShortUrlResponse> content = urlPage.getContent()
                .stream()
                .map(url -> urlMapper.toResponse(url, baseUrl))
                .toList();

        return PageResponse.of(
                content,
                urlPage.getNumber(),
                urlPage.getSize(),
                urlPage.getTotalElements(),
                urlPage.getTotalPages()
        );
    }

    @Override
    @Transactional
    public void deleteUrl(Long id) {
        Url url = urlRepository.findById(id)
                .orElseThrow(() -> new UrlNotFoundException("URL not found with ID: " + id));

        User currentUser = SecurityUtils.getCurrentUserOrNull();
        if (currentUser != null && currentUser.getRole() != Role.ADMIN) {
            if (url.getUser() == null || !url.getUser().getId().equals(currentUser.getId())) {
                throw new AccessDeniedException("You do not have permission to delete this URL");
            }
        }

        // Delete from database
        urlRepository.delete(url);
        log.info("Deleted URL entity with ID: {} and short code: {}", id, url.getShortCode());

        // Evict from Redis cache on deletion
        redisCacheService.evictUrl(url.getShortCode());
    }

    private String generateUniqueShortCode() {
        for (int attempt = 0; attempt < MAX_GENERATION_ATTEMPTS; attempt++) {
            String code = shortCodeGenerator.generateShortCode();
            if (!RESERVED_ALIASES.contains(code.toLowerCase()) && !urlRepository.existsByShortCode(code)) {
                return code;
            }
        }
        throw new IllegalStateException("Failed to generate a unique short code after multiple attempts");
    }

    private void incrementClickCountSilently(String shortCode) {
        try {
            urlRepository.findByShortCode(shortCode).ifPresent(url -> {
                urlRepository.incrementClickCount(url.getId());
            });
        } catch (Exception e) {
            log.error("Failed to increment click count for cached shortCode {}: {}", shortCode, e.getMessage());
        }
    }
}
