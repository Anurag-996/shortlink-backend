package com.shortlink.service;

import com.shortlink.dto.redis.CachedUrl;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.dao.DataAccessException;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.json.JsonMapper;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Optional;

// Dedicated service for managing expiration-aware short URL caching in Redis with self-healing invalidation.
@Slf4j
@Service
public class RedisCacheService {

    private static final String CACHE_KEY_PREFIX = "shortlink:url:";
    private final RedisTemplate<String, String> redisTemplate;
    private final JsonMapper jsonMapper;
    private final long ttlSeconds;

    public RedisCacheService(
            RedisTemplate<String, String> redisTemplate,
            JsonMapper jsonMapper,
            @Value("${app.redis.ttl-seconds:86400}") long ttlSeconds) {
        this.redisTemplate = redisTemplate;
        this.jsonMapper = jsonMapper;
        this.ttlSeconds = ttlSeconds;
    }

    // Fetches CachedUrl object from Redis cache, evicting incompatible/stale JSON and falling back to DB.
    public Optional<CachedUrl> getCachedUrl(String shortCode) {
        String key = buildKey(shortCode);
        try {
            String json = redisTemplate.opsForValue().get(key);
            if (json == null || json.isBlank()) {
                return Optional.empty();
            }

            CachedUrl cachedUrl = jsonMapper.readValue(json, CachedUrl.class);
            return Optional.of(cachedUrl);
        } catch (JacksonException e) {
            log.warn("Corrupted or incompatible cache entry for key [{}]; evicting from Redis: {}", shortCode, e.getMessage());
            evictUrl(shortCode);
            return Optional.empty();
        } catch (DataAccessException e) {
            log.error("Failed to read key {} from Redis cache: {}", shortCode, e.getMessage());
            return Optional.empty();
        }
    }

    // Caches shortCode -> CachedUrl mapping in Redis with dynamic TTL matching URL expiration.
    public void cacheUrl(String shortCode, String originalUrl, LocalDateTime expiresAt) {
        try {
            Duration effectiveTtl;
            Duration configuredTtl = Duration.ofSeconds(ttlSeconds);

            if (expiresAt == null) {
                effectiveTtl = configuredTtl;
            } else {
                LocalDateTime now = LocalDateTime.now();
                if (!expiresAt.isAfter(now)) {
                    log.debug("URL with shortCode [{}] is already expired; skipping Redis caching", shortCode);
                    return;
                }

                long remainingSeconds = Duration.between(now, expiresAt).getSeconds();
                if (remainingSeconds <= 0) {
                    return;
                }

                effectiveTtl = Duration.ofSeconds(Math.min(configuredTtl.getSeconds(), remainingSeconds));
            }

            CachedUrl cachedUrl = new CachedUrl(originalUrl, expiresAt);
            String json = jsonMapper.writeValueAsString(cachedUrl);
            String key = buildKey(shortCode);

            redisTemplate.opsForValue().set(key, json, effectiveTtl);
            log.debug("Cached short code [{}] with effective TTL {}", shortCode, effectiveTtl);
        } catch (JacksonException | DataAccessException e) {
            log.error("Failed to cache key {} in Redis: {}", shortCode, e.getMessage());
        }
    }

    // Evicts cache entry on URL deletion or expiration.
    public void evictUrl(String shortCode) {
        try {
            String key = buildKey(shortCode);
            Boolean deleted = redisTemplate.delete(key);
            log.debug("Evicted short code [{}] from cache: {}", shortCode, deleted);
        } catch (DataAccessException e) {
            log.error("Failed to evict key {} from Redis: {}", shortCode, e.getMessage());
        }
    }

    private String buildKey(String shortCode) {
        return CACHE_KEY_PREFIX + shortCode;
    }
}
