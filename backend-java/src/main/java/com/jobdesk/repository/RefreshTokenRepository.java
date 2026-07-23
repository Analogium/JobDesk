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

    /** Suppression du compte : on retire les sessions avant l'utilisateur lui-même. */
    void deleteByUser(User user);

    /** Révoque toutes les sessions d'un user (rejeu détecté, ou déconnexion globale). */
    @Modifying
    @Query("update RefreshToken t set t.revokedAt = :now where t.user = :user and t.revokedAt is null")
    void revokeAllForUser(User user, LocalDateTime now);

    /**
     * Purge les jetons qui ne peuvent plus servir : expirés, ou révoqués de longue date.
     * On garde brièvement les révoqués récents, car c'est leur présence qui permet de
     * détecter un rejeu (cf. {@code RefreshTokenService.rotate}).
     */
    @Modifying
    @Query("delete from RefreshToken t where t.expiresAt < :now or (t.revokedAt is not null and t.revokedAt < :revokedBefore)")
    int deleteUnusable(LocalDateTime now, LocalDateTime revokedBefore);
}
