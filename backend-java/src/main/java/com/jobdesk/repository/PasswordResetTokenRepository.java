package com.jobdesk.repository;

import com.jobdesk.domain.PasswordResetToken;
import com.jobdesk.domain.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

public interface PasswordResetTokenRepository extends JpaRepository<PasswordResetToken, UUID> {

    Optional<PasswordResetToken> findByTokenHash(String tokenHash);

    /** Une nouvelle demande invalide les précédentes : un seul lien actif par compte. */
    void deleteByUser(User user);

    /** Purge les liens qui ne peuvent plus servir : expirés ou déjà consommés. */
    @Modifying
    @Query("delete from PasswordResetToken t where t.expiresAt < :now or t.usedAt is not null")
    int deleteUnusable(LocalDateTime now);
}
