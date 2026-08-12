package com.shortlink.security.ratelimit;

import org.springframework.context.annotation.Configuration;

// Rate limit configuration rules and route profiles using Token Bucket semantics.
@Configuration
public class RateLimiterConfig {

    public static final boolean ENABLED = true;
    public static final boolean TRUST_PROXY = false;

    // Route-specific Token Bucket configurations: TokenBucketLimit(capacity, refillTokensPerSecond)
    public static final TokenBucketLimit AUTH_LOGIN = new TokenBucketLimit(5, 5.0 / 60.0);      // 5 burst capacity, 1 token refilled every 12s
    public static final TokenBucketLimit AUTH_REFRESH = new TokenBucketLimit(10, 10.0 / 60.0);  // 10 burst capacity, 1 token refilled every 6s
    public static final TokenBucketLimit AUTH_LOGOUT = new TokenBucketLimit(10, 10.0 / 60.0);   // 10 burst capacity, 1 token refilled every 6s
    public static final TokenBucketLimit URL_CREATION = new TokenBucketLimit(30, 30.0 / 60.0);  // 30 burst capacity, 0.5 tokens/s (1 token every 2s)
    public static final TokenBucketLimit URL_REDIRECT = new TokenBucketLimit(60, 60.0 / 60.0);  // 60 burst capacity, 1.0 token/s
    public static final TokenBucketLimit ANALYTICS = new TokenBucketLimit(30, 30.0 / 60.0);     // 30 burst capacity, 0.5 tokens/s
    public static final TokenBucketLimit DEFAULT_LIMIT = new TokenBucketLimit(60, 60.0 / 60.0); // 60 burst capacity, 1.0 token/s

    public record TokenBucketLimit(int capacity, double refillTokensPerSecond) {
        public TokenBucketLimit(int capacity, int windowSeconds) {
            this(capacity, (double) capacity / (double) windowSeconds);
        }
    }
}
