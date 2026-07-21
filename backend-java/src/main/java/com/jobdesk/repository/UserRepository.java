package com.jobdesk.repository;

import com.jobdesk.domain.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface UserRepository extends JpaRepository<User, UUID> {
    Optional<User> findByEmail(String email);

    /**
     * Les emails sont normalisés en minuscules à l'inscription, mais ceux créés par
     * le flux Google dépendent de ce que renvoie Google : on compare sans casse pour
     * qu'un même utilisateur ne puisse pas se retrouver avec deux comptes.
     */
    Optional<User> findByEmailIgnoreCase(String email);

    /** Utilisateurs ayant connecté Gmail (pour le scan périodique). */
    List<User> findByGmailTokenIsNotNull();
}
