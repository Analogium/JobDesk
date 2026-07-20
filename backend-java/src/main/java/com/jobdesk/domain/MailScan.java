package com.jobdesk.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "mail_scan")
public class MailScan {

    @Id
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(nullable = false)
    private LocalDateTime scannedAt;

    private Integer mailsAnalyzed;

    private Integer matchesFound;

    @Column(length = 20, nullable = false)
    private String status = "success";

    @Column(columnDefinition = "text")
    private String errorMessage;

    @PrePersist
    void onPrePersist() {
        if (id == null) {
            id = UUID.randomUUID();
        }
        if (scannedAt == null) {
            scannedAt = LocalDateTime.now();
        }
    }

    public UUID getId() {
        return id;
    }

    public User getUser() {
        return user;
    }

    public void setUser(User user) {
        this.user = user;
    }

    public LocalDateTime getScannedAt() {
        return scannedAt;
    }

    public void setScannedAt(LocalDateTime scannedAt) {
        this.scannedAt = scannedAt;
    }

    public Integer getMailsAnalyzed() {
        return mailsAnalyzed;
    }

    public void setMailsAnalyzed(Integer mailsAnalyzed) {
        this.mailsAnalyzed = mailsAnalyzed;
    }

    public Integer getMatchesFound() {
        return matchesFound;
    }

    public void setMatchesFound(Integer matchesFound) {
        this.matchesFound = matchesFound;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getErrorMessage() {
        return errorMessage;
    }

    public void setErrorMessage(String errorMessage) {
        this.errorMessage = errorMessage;
    }
}
