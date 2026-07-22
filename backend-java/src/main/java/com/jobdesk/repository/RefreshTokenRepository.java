package com.jobdesk.repository;

import com.jobdesk.domain.RefreshToken;
import com.jobdesk.domain.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

public interface RefreshTokenRepository extends JpaRepository<RefreshToken, UUID> {

    /**
     * {@code join fetch} sur le user : le JWT est généré dans le contrôleur, hors
     * transaction, donc le user doit être chargé ici et non laissé en proxy lazy
     * (sinon {@code LazyInitializationException}).
     */
    @Query("select t from RefreshToken t join fetch t.user where t.tokenHash = :tokenHash")
    Optional<RefreshToken> findByTokenHash(String tokenHash);

    /** Révoque toutes les sessions d'un user (rejeu détecté, ou déconnexion globale). */
    @Modifying
    @Query("update RefreshToken t set t.revokedAt = :now where t.user = :user and t.revokedAt is null")
    void revokeAllForUser(User user, LocalDateTime now);
}
