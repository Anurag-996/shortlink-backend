package com.shortlink.user;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

// Spring Data JPA Repository for managing User entities.
public interface UserRepository extends JpaRepository<User, Long> {

    // Finds a user by their unique email address.
    Optional<User> findByEmail(String email);

    // Checks whether an account exists for the given email.
    boolean existsByEmail(String email);
}
