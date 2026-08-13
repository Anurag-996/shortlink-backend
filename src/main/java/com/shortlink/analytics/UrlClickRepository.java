package com.shortlink.analytics;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

// Spring Data JPA Repository for managing UrlClick analytics entities.
public interface UrlClickRepository extends JpaRepository<UrlClick, Long> {

    // Checks if a click record exists with the given unique event ID.
    boolean existsByEventId(UUID eventId);

    // Counts total clicks for a specific short code.
    long countByShortCode(String shortCode);

    // Finds a click record by unique event ID.
    Optional<UrlClick> findByEventId(UUID eventId);

    // Deletes click analytics records for a list of short codes.
    void deleteByShortCodeIn(List<String> shortCodes);

    // Deletes click analytics records for a single short code.
    void deleteByShortCode(String shortCode);
}
