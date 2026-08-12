package com.shortlink.service;

import org.springframework.stereotype.Component;

import java.security.SecureRandom;

// Utility component for generating cryptographically secure random short codes using Base62.
@Component
public class ShortCodeGenerator {

    private static final String BASE62_ALPHABET = "0123456789abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ";
    private static final int DEFAULT_CODE_LENGTH = 7;
    private final SecureRandom secureRandom = new SecureRandom();

    // Generates a random Base62 short code of default length (7 characters).
    public String generateShortCode() {
        return generateShortCode(DEFAULT_CODE_LENGTH);
    }

    // Generates a random Base62 short code of specified length.
    public String generateShortCode(int length) {
        StringBuilder sb = new StringBuilder(length);
        for (int i = 0; i < length; i++) {
            int randomIndex = secureRandom.nextInt(BASE62_ALPHABET.length());
            sb.append(BASE62_ALPHABET.charAt(randomIndex));
        }
        return sb.toString();
    }
}
