package com.jobdesk.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import org.hibernate.annotations.OnDelete;
import org.hibernate.annotations.OnDeleteAction;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Lien de partage en lecture seule des candidatures du titulaire.
 *
 * <p>Un tiers présentant le {@code token} accède, sans authentification, à une vue
 * réduite des candidatures (sans les notes personnelles ni les contacts). Un seul lien
 * par compte (contrainte {@code UNIQUE} sur {@code user_id}) : en régénérer un remplace
 * le précédent.
 *
 * <p>Contrairement aux jetons de session et de réinitialisation, le token est conservé
 * <strong>en clair</strong> : il doit pouvoir être réaffiché pour être renvoyé au même
 * destinataire. Le périmètre à faible sensibilité (lecture seule, données réduites)
 * justifie cet écart.
 */
@Entity
@Table(name = "share_link")
public class ShareLink {

    @Id
    private UUID id;

    /** @OnDelete : ON DELETE CASCADE côté schéma, comme la migration Postgres. */
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false, unique = true)
    @OnDelete(action = OnDeleteAction.CASCADE)
    private User user;

    @Column(nullable = false, unique = true, length = 64)
    private String token;

    @Column(nullable = false)
    private LocalDateTime createdAt;

    /** {@code null} = le lien n'expire jamais (choix du titulaire à la génération). */
    private LocalDateTime expiresAt;

    @PrePersist
    void onPrePersist() {
        if (id == null) {
            id = UUID.randomUUID();
        }
        createdAt = LocalDateTime.now();
    }

    /** Vrai tant que le lien n'a pas d'échéance dépassée. */
    public boolean isValid(LocalDateTime now) {
        return expiresAt == null || expiresAt.isAfter(now);
    }

    // ─── Getters / setters ───────────────────────────────────────────────────

    public UUID getId() {
        return id;
    }

    public User getUser() {
        return user;
    }

    public void setUser(User user) {
        this.user = user;
    }

    public String getToken() {
        return token;
    }

    public void setToken(String token) {
        this.token = token;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public LocalDateTime getExpiresAt() {
        return expiresAt;
    }

    public void setExpiresAt(LocalDateTime expiresAt) {
        this.expiresAt = expiresAt;
    }
}
