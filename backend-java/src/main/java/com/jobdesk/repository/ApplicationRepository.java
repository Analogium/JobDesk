package com.jobdesk.repository;

import com.jobdesk.domain.Application;
import com.jobdesk.domain.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Le filtrage par utilisateur (équivalent de l'ancien {@code ApplicationUserExtension})
 * est porté ici : toute lecture/écriture passe par le user courant.
 */
public interface ApplicationRepository
        extends JpaRepository<Application, UUID>, JpaSpecificationExecutor<Application> {

    Page<Application> findByUser(User user, Pageable pageable);

    List<Application> findByUser(User user);

    Optional<Application> findByIdAndUser(UUID id, User user);
}
