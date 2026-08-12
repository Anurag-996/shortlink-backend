package com.shortlink.security.ratelimit;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataAccessException;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.RedisScript;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.List;

// Redis-backed atomic Token Bucket rate limiter using SHA-256 hashed client identities and atomic Lua evaluation.
@Slf4j
@Component
@RequiredArgsConstructor
public class RedisRateLimiter {

    private static final String SCRIPT = """
            local key = KEYS[1]
            local capacity = tonumber(ARGV[1])
            local refill_rate = tonumber(ARGV[2])
            local requested = 1

            -- 1. Obtain current Redis server time with microsecond precision
            local time_arr = redis.call('TIME')
            local now = tonumber(time_arr[1]) + (tonumber(time_arr[2]) / 1000000)

            -- 2. Fetch bucket state from Redis Hash
            local data = redis.call('HMGET', key, 'tokens', 'last_refill_time')
            local current_tokens = tonumber(data[1])
            local last_refill_time = tonumber(data[2])

            -- 3. Initialize or refill bucket based on elapsed time
            if not current_tokens or not last_refill_time then
                current_tokens = capacity
                last_refill_time = now
            else
                local elapsed = math.max(0, now - last_refill_time)
                local generated_tokens = elapsed * refill_rate
                current_tokens = math.min(capacity, current_tokens + generated_tokens)
                last_refill_time = now
            end

            -- 4. Evaluate consumption and calculate retry-after TTL if exhausted
            local allowed = 0
            local remaining = current_tokens
            local retry_after = 0

            if current_tokens >= requested then
                allowed = 1
                current_tokens = current_tokens - requested
                remaining = math.floor(current_tokens)
            else
                allowed = 0
                remaining = math.floor(current_tokens)
                local missing_tokens = requested - current_tokens
                retry_after = math.ceil(missing_tokens / refill_rate)
                if retry_after < 1 then
                    retry_after = 1
                end
            end

            -- 5. Atomically persist updated bucket state
            redis.call('HSET', key, 'tokens', current_tokens, 'last_refill_time', last_refill_time)

            -- 6. Refresh TTL (capacity / refill_rate * 2) with a 60-second floor so idle buckets expire cleanly
            local full_refill_seconds = math.ceil(capacity / refill_rate)
            local ttl = math.max(60, full_refill_seconds * 2)
            redis.call('EXPIRE', key, ttl)

            return tostring(allowed) .. ":" .. tostring(remaining) .. ":" .. tostring(retry_after)
            """;

    private final StringRedisTemplate redisTemplate;
    private final RedisScript<String> redisScript = RedisScript.of(SCRIPT, String.class);

    // Evaluates rate limit atomically against Redis using Token Bucket algorithm with SHA-256 hashed client identity.
    public RateLimitResult isAllowed(String routeCategory, String clientKey, int capacity, double refillTokensPerSecond) {
        String hashedClient = hashClientKey(clientKey);
        String redisKey = "gateway:rl:" + routeCategory + ":" + hashedClient;

        try {
            String result = redisTemplate.execute(
                    redisScript,
                    List.of(redisKey),
                    String.valueOf(capacity),
                    String.valueOf(refillTokensPerSecond)
            );

            if (result != null) {
                String[] parts = result.split(":");
                if (parts.length >= 3) {
                    long allowedFlag = Long.parseLong(parts[0]);
                    long remaining = Long.parseLong(parts[1]);
                    long retryAfter = Long.parseLong(parts[2]);

                    return new RateLimitResult(
                            allowedFlag == 1L,
                            capacity,
                            Math.max(0, remaining),
                            allowedFlag == 1L ? 0 : Math.max(1, retryAfter)
                    );
                }
            }
        } catch (DataAccessException | NumberFormatException e) {
            // Fail-open: log error server-side and allow traffic on Redis connectivity failures
            log.error("Redis rate limiter execution failed for category [{}]: {}", routeCategory, e.getMessage());
            return new RateLimitResult(true, capacity, Math.max(0, capacity - 1), 0);
        }

        return new RateLimitResult(true, capacity, Math.max(0, capacity - 1), 0);
    }

    private String hashClientKey(String clientKey) {
        if (clientKey == null) {
            clientKey = "anonymous";
        }
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(clientKey.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(hash);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 algorithm not available in JVM", e);
        }
    }

    public record RateLimitResult(
        boolean allowed,
        long limit,
        long remaining,
        long retryAfterSeconds
    ) {}
}
