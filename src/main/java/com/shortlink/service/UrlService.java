package com.shortlink.service;

import com.shortlink.dto.request.CreateShortUrlRequest;
import com.shortlink.dto.request.UpdateUrlRequest;
import com.shortlink.dto.response.PageResponse;
import com.shortlink.dto.response.ShortUrlResponse;

// Interface defining business logic operations for URL management with pagination and sorting.
public interface UrlService {

    // Creates a shortened URL record from original URL or custom alias.
    ShortUrlResponse createShortUrl(CreateShortUrlRequest request);

    // Updates an existing shortened URL's destination and optional expiration, evicting cache.
    ShortUrlResponse updateUrl(Long id, UpdateUrlRequest request);

    // Resolves shortCode to originalUrl using Cache-Aside strategy, incrementing click count.
    String getOriginalUrlAndIncrementClick(String shortCode);

    // Retrieves URL mapping details by shortCode.
    ShortUrlResponse getUrlByShortCode(String shortCode);

    // Retrieves paginated and sorted short URL records.
    PageResponse<ShortUrlResponse> getAllUrls(int page, int size, String sortBy, String direction);

    // Deletes a short URL by its database primary key ID and evicts cache entry.
    void deleteUrl(Long id);
}
