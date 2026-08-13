package com.shortlink.repository;

import java.time.Instant;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.shortlink.entity.RefreshToken;
import com.shortlink.user.User;

// Spring Data JPA Repository for managing RefreshToken entities.
public interface RefreshTokenRepository extends JpaRepository<RefreshToken, Long> {

    // Finds a refresh token entity by its secure token string.
    Optional<RefreshToken> findByToken(String token);

    // Deletes a specific refresh token by string.
    void deleteByToken(String token);

    // Revokes/deletes all active refresh tokens for a user.
    void deleteByUser(User user);

    // Purges expired refresh tokens.
    @Modifying
    @Query("DELETE FROM RefreshToken r WHERE r.expiresAt < :now")
    void deleteExpiredTokens(@Param("now") Instant now);
}
