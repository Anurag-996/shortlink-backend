package com.shortlink.controller;

import java.net.URI;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.shortlink.dto.request.CreateShortUrlRequest;
import com.shortlink.dto.request.UpdateUrlRequest;
import com.shortlink.dto.response.PageResponse;
import com.shortlink.dto.response.ShortUrlResponse;
import com.shortlink.service.UrlService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

// REST Controller for managing short URLs with pagination and sorting.
@RestController
@RequestMapping("/api/v1/urls")
@RequiredArgsConstructor
public class UrlController {

    private final UrlService urlService;

    // POST /api/v1/urls - Creates a new short URL.
    @PostMapping
    public ResponseEntity<ShortUrlResponse> createShortUrl(@Valid @RequestBody CreateShortUrlRequest request) {
        ShortUrlResponse response = urlService.createShortUrl(request);
        return ResponseEntity
                .status(HttpStatus.CREATED)
                .location(URI.create(response.shortUrl()))
                .body(response);
    }

    // GET /api/v1/urls - Retrieves paginated and sorted short URLs.
    @GetMapping
    public ResponseEntity<PageResponse<ShortUrlResponse>> getAllUrls(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false, defaultValue = "createdAt") String sortBy,
            @RequestParam(required = false, defaultValue = "desc") String direction
    ) {
        PageResponse<ShortUrlResponse> urls = urlService.getAllUrls(page, size, sortBy, direction);
        return ResponseEntity.ok(urls);
    }

    // GET /api/v1/urls/{shortCode} - Retrieves short URL details by code.
    @GetMapping("/{shortCode}")
    public ResponseEntity<ShortUrlResponse> getUrlDetails(@PathVariable String shortCode) {
        ShortUrlResponse response = urlService.getUrlByShortCode(shortCode);
        return ResponseEntity.ok(response);
    }

    // PUT /api/v1/urls/{id} - Updates an existing short URL destination and optional expiration.
    @PutMapping("/{id}")
    public ResponseEntity<ShortUrlResponse> updateUrl(
            @PathVariable Long id,
            @Valid @RequestBody UpdateUrlRequest request
    ) {
        ShortUrlResponse response = urlService.updateUrl(id, request);
        return ResponseEntity.ok(response);
    }

    // DELETE /api/v1/urls/{id} - Deletes a short URL by database primary key ID.
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteUrl(@PathVariable Long id) {
        urlService.deleteUrl(id);
        return ResponseEntity.noContent().build();
    }
}
