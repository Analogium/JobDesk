package com.jobdesk.repository;

import com.jobdesk.domain.PasswordResetToken;
import com.jobdesk.domain.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface PasswordResetTokenRepository extends JpaRepository<PasswordResetToken, UUID> {

    Optional<PasswordResetToken> findByTokenHash(String tokenHash);

    /** Une nouvelle demande invalide les précédentes : un seul lien actif par compte. */
    void deleteByUser(User user);
}
