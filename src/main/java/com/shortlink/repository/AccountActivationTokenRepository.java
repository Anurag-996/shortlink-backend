package com.shortlink.repository;

import com.shortlink.entity.AccountActivationToken;
import com.shortlink.user.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.Optional;

// Repository for managing AccountActivationToken records.
public interface AccountActivationTokenRepository extends JpaRepository<AccountActivationToken, Long> {

    Optional<AccountActivationToken> findByTokenHash(String tokenHash);

    Optional<AccountActivationToken> findByUser(User user);

    void deleteByUser(User user);

    @Modifying
    @Query("DELETE FROM AccountActivationToken a WHERE a.expiresAt < :now")
    void deleteByExpiresAtBefore(@Param("now") LocalDateTime now);
}
