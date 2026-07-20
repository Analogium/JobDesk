package com.jobdesk.repository;

import com.jobdesk.domain.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface UserRepository extends JpaRepository<User, UUID> {
    Optional<User> findByEmail(String email);

    /** Utilisateurs ayant connecté Gmail (pour le scan périodique). */
    List<User> findByGmailTokenIsNotNull();
}
