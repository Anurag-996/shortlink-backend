package com.shortlink.service;

import org.springframework.stereotype.Component;

import com.shortlink.dto.request.CreateShortUrlRequest;
import com.shortlink.dto.response.ShortUrlResponse;
import com.shortlink.entity.Url;

// Mapper component for converting between Url entities and DTOs with auto-normalization.
@Component
public class UrlMapper {

    // Converts a Url entity to a ShortUrlResponse DTO.
    public ShortUrlResponse toResponse(Url url, String baseUrl) {
        if (url == null) {
            return null;
        }

        String formattedBaseUrl = baseUrl != null && baseUrl.endsWith("/")
            ? baseUrl.substring(0, baseUrl.length() - 1)
            : baseUrl;

        String fullShortUrl = (formattedBaseUrl != null ? formattedBaseUrl : "") + "/" + url.getShortCode();

        return new ShortUrlResponse(
            url.getId(),
            url.getOriginalUrl(),
            url.getShortCode(),
            fullShortUrl,
            url.getClickCount(),
            url.getExpiresAt(),
            url.getCreatedAt()
        );
    }

    // Converts a CreateShortUrlRequest DTO to a Url entity, automatically prepending https:// if protocol is omitted.
    public Url toEntity(CreateShortUrlRequest request, String shortCode) {
        if (request == null) {
            return null;
        }

        String normalizedUrl = request.originalUrl().trim();
        if (!normalizedUrl.matches("(?i)^https?://.*")) {
            if (normalizedUrl.matches("(?i)^(localhost|127\\.\\d+\\.\\d+\\.\\d+)(:\\d+)?(/.*)?$")) {
                normalizedUrl = "http://" + normalizedUrl;
            } else {
                normalizedUrl = "https://" + normalizedUrl;
            }
        }

        return Url.builder()
            .originalUrl(normalizedUrl)
            .shortCode(shortCode)
            .clickCount(0L)
            .expiresAt(request.expiresAt())
            .build();
    }
}
