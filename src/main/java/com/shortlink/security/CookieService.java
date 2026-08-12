package com.shortlink.security;

import com.shortlink.config.CookieProperties;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.Arrays;
import java.util.Optional;

// Service responsible for constructing secure HttpOnly cookies for refresh token transmission.
@Service
@RequiredArgsConstructor
public class CookieService {

    public static final String REFRESH_TOKEN_COOKIE_NAME = "refreshToken";
    private final CookieProperties cookieProperties;

    // Attaches an HttpOnly refresh token cookie to the HTTP response.
    public void setRefreshTokenCookie(HttpServletResponse response, String token, long maxAgeMillis) {
        ResponseCookie cookie = ResponseCookie.from(REFRESH_TOKEN_COOKIE_NAME, token)
                .httpOnly(true)
                .secure(cookieProperties.secure())
                .path(cookieProperties.path())
                .maxAge(Duration.ofMillis(maxAgeMillis))
                .sameSite(cookieProperties.sameSite())
                .build();

        response.addHeader(HttpHeaders.SET_COOKIE, cookie.toString());
    }

    // Clears the refresh token cookie by setting Max-Age=0.
    public void clearRefreshTokenCookie(HttpServletResponse response) {
        ResponseCookie cookie = ResponseCookie.from(REFRESH_TOKEN_COOKIE_NAME, "")
                .httpOnly(true)
                .secure(cookieProperties.secure())
                .path(cookieProperties.path())
                .maxAge(0)
                .sameSite(cookieProperties.sameSite())
                .build();

        response.addHeader(HttpHeaders.SET_COOKIE, cookie.toString());
    }

    // Extracts refresh token value from request cookies.
    public Optional<String> extractRefreshToken(HttpServletRequest request) {
        if (request.getCookies() == null) {
            return Optional.empty();
        }

        return Arrays.stream(request.getCookies())
                .filter(cookie -> REFRESH_TOKEN_COOKIE_NAME.equals(cookie.getName()))
                .map(Cookie::getValue)
                .findFirst();
    }
}
