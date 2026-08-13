package com.shortlink.user;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

// Spring Data JPA Repository for managing User entities.
public interface UserRepository extends JpaRepository<User, Long> {

    // Finds a user by their unique email address.
    Optional<User> findByEmail(String email);

    // Checks whether an account exists for the given email.
    boolean existsByEmail(String email);

    // Finds all users whose account deletion grace period has expired.
    List<User> findAllByDeletionPendingTrueAndDeletionScheduledAtBefore(Instant now);

    // Counts active enabled users.
    long countByEnabledTrue();

    // Counts disabled users.
    long countByEnabledFalse();

    // Counts users pending deletion.
    long countByDeletionPendingTrue();

    // Counts users by role.
    long countByRole(Role role);

    // Counts users registered after a specific timestamp.
    long countByCreatedAtAfter(Instant since);

    // Searches users by name or email (case-insensitive) with pagination.
    @Query("SELECT u FROM User u WHERE LOWER(u.name) LIKE LOWER(CONCAT('%', :search, '%')) OR LOWER(u.email) LIKE LOWER(CONCAT('%', :search, '%'))")
    Page<User> searchUsers(@Param("search") String search, Pageable pageable);

    // Aggregates user registrations daily for Admin Growth Chart.
    @Query("SELECT FUNCTION('to_char', u.createdAt, 'YYYY-MM-DD'), COUNT(u) FROM User u WHERE u.createdAt >= :since GROUP BY FUNCTION('to_char', u.createdAt, 'YYYY-MM-DD') ORDER BY FUNCTION('to_char', u.createdAt, 'YYYY-MM-DD') ASC")
    List<Object[]> getNewUsersTimeSeriesDaily(@Param("since") Instant since);

    // Finds most recently registered users for activity feed.
    List<User> findTop10ByOrderByCreatedAtDesc();
}
