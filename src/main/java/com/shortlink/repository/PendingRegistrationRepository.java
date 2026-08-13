package com.shortlink.repository;

import com.shortlink.entity.PendingRegistration;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.Optional;

// Repository for managing PendingRegistration records.
public interface PendingRegistrationRepository extends JpaRepository<PendingRegistration, Long> {

    Optional<PendingRegistration> findByEmail(String email);

    Optional<PendingRegistration> findByVerificationTokenHash(String verificationTokenHash);

    boolean existsByEmail(String email);

    void deleteByEmail(String email);

    @Modifying
    @Query("DELETE FROM PendingRegistration p WHERE p.expiresAt < :now")
    void deleteByExpiresAtBefore(@Param("now") LocalDateTime now);
}
