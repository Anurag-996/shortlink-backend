package com.shortlink.security.jwt;

import com.shortlink.config.JwtProperties;
import com.shortlink.user.User;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.util.Base64;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.function.Function;

// Service responsible for generating, signing, and validating JWT access tokens and refresh token strings.
@Slf4j
@Service
public class JwtService {

    private final JwtProperties jwtProperties;
    private final SecretKey secretKey;
    private final SecureRandom secureRandom = new SecureRandom();

    public JwtService(JwtProperties jwtProperties) {
        this.jwtProperties = jwtProperties;
        this.secretKey = Keys.hmacShaKeyFor(jwtProperties.secret().getBytes(StandardCharsets.UTF_8));
    }

    // Generates a signed JWT access token for the given authenticated User.
    public String generateAccessToken(User user) {
        long nowMillis = System.currentTimeMillis();
        Date issuedAt = new Date(nowMillis);
        Date expiration = new Date(nowMillis + jwtProperties.accessTokenExpiration());

        return Jwts.builder()
                .subject(user.getEmail())
                .claim("userId", user.getId())
                .claim("role", user.getRole().name())
                .issuedAt(issuedAt)
                .expiration(expiration)
                .signWith(secretKey)
                .compact();
    }

    // Generates a cryptographically secure, random, URL-safe refresh token string.
    public String generateRefreshTokenString() {
        byte[] randomBytes = new byte[48];
        secureRandom.nextBytes(randomBytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(randomBytes);
    }

    // Extracts subject (email) from access JWT token.
    public String extractUsername(String token) {
        return extractClaim(token, Claims::getSubject);
    }

    // Extracts roles / authorities list from the single 'role' token claim.
    public List<String> extractRoles(String token) {
        Claims claims = extractAllClaims(token);
        if (claims == null) {
            return Collections.emptyList();
        }

        Object roleObj = claims.get("role");
        if (roleObj instanceof String roleStr && !roleStr.isBlank()) {
            return List.of(roleStr.trim());
        }

        return Collections.emptyList();
    }

    // Validates whether the token signature is authentic and not expired.
    public boolean isTokenValid(String token) {
        try {
            Claims claims = extractAllClaims(token);
            return claims != null && !claims.getExpiration().before(new Date());
        } catch (JwtException | IllegalArgumentException e) {
            log.warn("Invalid JWT token: {}", e.getMessage());
            return false;
        }
    }

    public <T> T extractClaim(String token, Function<Claims, T> claimsResolver) {
        Claims claims = extractAllClaims(token);
        return claims != null ? claimsResolver.apply(claims) : null;
    }

    private Claims extractAllClaims(String token) {
        try {
            return Jwts.parser()
                    .verifyWith(secretKey)
                    .build()
                    .parseSignedClaims(token)
                    .getPayload();
        } catch (JwtException | IllegalArgumentException e) {
            log.warn("Failed to parse JWT claims: {}", e.getMessage());
            return null;
        }
    }

    public long getAccessTokenExpirationMillis() {
        return jwtProperties.accessTokenExpiration();
    }

    public long getRefreshTokenExpirationMillis() {
        return jwtProperties.refreshTokenExpiration();
    }
}
