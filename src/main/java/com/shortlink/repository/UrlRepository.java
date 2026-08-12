package com.shortlink.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.shortlink.entity.Url;

// Spring Data JPA Repository for managing Url entities.
public interface UrlRepository extends JpaRepository<Url, Long> {

    // Finds a Url entity by its unique short code.
    Optional<Url> findByShortCode(String shortCode);

    // Checks if a Url entity exists with the given short code.
    boolean existsByShortCode(String shortCode);

    // Atomically increments the click count for a URL given its ID.
    @Modifying
    @Query("UPDATE Url u SET u.clickCount = u.clickCount + 1 WHERE u.id = :id")
    void incrementClickCount(@Param("id") Long id);
}
