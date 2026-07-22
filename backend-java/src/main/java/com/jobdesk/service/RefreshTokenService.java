package com.jobdesk.service;

import com.jobdesk.domain.RefreshToken;
import com.jobdesk.domain.User;
import com.jobdesk.repository.RefreshTokenRepository;
import com.jobdesk.security.Tokens;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;

/**
 * Cycle de vie des refresh tokens : émission, rotation, révocation.
 *
 * <p>Un access token (JWT) est court et non révocable ; le refresh token, lui, est long,
 * stocké en base (haché) et donc révocable. À chaque rafraîchissement il tourne — l'ancien
 * est révoqué, un nouveau est émis — ce qui permet de détecter le rejeu d'un token volé.
 */
@Service
public class RefreshTokenService {

    private static final Logger log = LoggerFactory.getLogger(RefreshTokenService.class);

    /** Résultat d'une rotation : à qui appartient la session, et le nouveau refresh token. */
    public record Rotation(User user, String refreshToken) {
    }

    private final RefreshTokenRepository repository;
    private final long ttlSeconds;

    public RefreshTokenService(RefreshTokenRepository repository,
                               @Value("${app.refresh.ttl-seconds:2592000}") long ttlSeconds) {
        this.repository = repository;
        this.ttlSeconds = ttlSeconds;
    }

    /** Émet un nouveau refresh token pour {@code user} et renvoie sa valeur en clair. */
    @Transactional
    public String issue(User user) {
        String raw = Tokens.random();
        RefreshToken entity = new RefreshToken();
        entity.setUser(user);
        entity.setTokenHash(Tokens.sha256(raw));
        entity.setExpiresAt(LocalDateTime.now().plusSeconds(ttlSeconds));
        repository.save(entity);
        return raw;
    }

    /**
     * Valide un refresh token et le fait tourner. Lève 401 si le token est inconnu,
     * expiré ou déjà utilisé — ce dernier cas révoque en plus toutes les sessions du
     * compte, car un token révoqué qui ressurgit signale un vol.
     *
     * <p>{@code noRollbackFor} : la détection de rejeu révoque puis lève 401 ; sans ça, le
     * rollback par défaut sur RuntimeException annulerait justement la révocation.
     */
    @Transactional(noRollbackFor = ResponseStatusException.class)
    public Rotation rotate(String rawToken) {
        LocalDateTime now = LocalDateTime.now();
        RefreshToken current = repository.findByTokenHash(Tokens.sha256(rawToken))
                .orElseThrow(RefreshTokenService::unauthorized);

        if (current.getRevokedAt() != null) {
            // Rejeu d'un token déjà consommé : on déconnecte toutes les sessions du compte.
            log.warn("Refresh token rejoué pour user {} — révocation globale", current.getUser().getId());
            repository.revokeAllForUser(current.getUser(), now);
            throw unauthorized();
        }
        if (!current.isUsable(now)) {
            throw unauthorized();
        }

        current.setRevokedAt(now);
        repository.save(current);

        String raw = issue(current.getUser());
        return new Rotation(current.getUser(), raw);
    }

    /** Révoque un refresh token (déconnexion). Silencieux si le token est déjà inconnu/révoqué. */
    @Transactional
    public void revoke(String rawToken) {
        if (rawToken == null || rawToken.isBlank()) {
            return;
        }
        repository.findByTokenHash(Tokens.sha256(rawToken)).ifPresent(token -> {
            if (token.getRevokedAt() == null) {
                token.setRevokedAt(LocalDateTime.now());
                repository.save(token);
            }
        });
    }

    private static ResponseStatusException unauthorized() {
        return new ResponseStatusException(HttpStatus.UNAUTHORIZED,
                "Session expirée, reconnectez-vous.");
    }
}
