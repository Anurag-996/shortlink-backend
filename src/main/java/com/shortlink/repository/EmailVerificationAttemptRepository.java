package com.shortlink.repository;

import com.shortlink.entity.EmailVerificationAttempt;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;

// Repository for managing email verification rate-limiting attempts.
public interface EmailVerificationAttemptRepository extends JpaRepository<EmailVerificationAttempt, Long> {

    long countByEmailAndSentAtAfter(String email, LocalDateTime after);

    @Modifying
    @Query("DELETE FROM EmailVerificationAttempt a WHERE a.sentAt < :threshold")
    void deleteBySentAtBefore(@Param("threshold") LocalDateTime threshold);
}
