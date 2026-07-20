package com.jobdesk.domain;

import com.jobdesk.domain.converter.EncryptedStringConverter;
import jakarta.persistence.Column;
import jakarta.persistence.Convert;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import org.hibernate.annotations.DynamicUpdate;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * {@code @DynamicUpdate} : seules les colonnes réellement modifiées sont écrites en UPDATE.
 * Essentiel pour les champs chiffrés : si un token ne peut pas être déchiffré (clé
 * différente), il reste null en mémoire mais NE doit PAS écraser la valeur en base.
 */
@Entity
@DynamicUpdate
@Table(name = "\"user\"")
public class User {

    @Id
    private UUID id;

    @Column(nullable = false, unique = true)
    private String email;

    @Column(nullable = false)
    private String name;

    @Column(length = 500)
    private String avatarUrl;

    @Column(columnDefinition = "text")
    private String googleToken;

    @Convert(converter = EncryptedStringConverter.class)
    @Column(columnDefinition = "text")
    private String gmailToken;

    @Convert(converter = EncryptedStringConverter.class)
    @Column(columnDefinition = "text")
    private String gmailRefreshToken;

    private LocalDateTime lastMailScanAt;

    @Column(nullable = false)
    private LocalDateTime createdAt;

    @Column(nullable = false)
    private LocalDateTime updatedAt;

    @PrePersist
    void onPrePersist() {
        if (id == null) {
            id = UUID.randomUUID();
        }
        LocalDateTime now = LocalDateTime.now();
        createdAt = now;
        updatedAt = now;
    }

    @PreUpdate
    void onPreUpdate() {
        updatedAt = LocalDateTime.now();
    }

    // ─── Getters / setters ───────────────────────────────────────────────────

    public UUID getId() {
        return id;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getAvatarUrl() {
        return avatarUrl;
    }

    public void setAvatarUrl(String avatarUrl) {
        this.avatarUrl = avatarUrl;
    }

    public String getGoogleToken() {
        return googleToken;
    }

    public void setGoogleToken(String googleToken) {
        this.googleToken = googleToken;
    }

    public String getGmailToken() {
        return gmailToken;
    }

    public void setGmailToken(String gmailToken) {
        this.gmailToken = gmailToken;
    }

    public String getGmailRefreshToken() {
        return gmailRefreshToken;
    }

    public void setGmailRefreshToken(String gmailRefreshToken) {
        this.gmailRefreshToken = gmailRefreshToken;
    }

    public LocalDateTime getLastMailScanAt() {
        return lastMailScanAt;
    }

    public void setLastMailScanAt(LocalDateTime lastMailScanAt) {
        this.lastMailScanAt = lastMailScanAt;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }
}
