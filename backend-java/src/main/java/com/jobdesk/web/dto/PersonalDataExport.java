package com.jobdesk.web.dto;

import com.jobdesk.domain.MailScan;
import com.jobdesk.domain.ShareLink;
import com.jobdesk.domain.User;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

/**
 * Export de l'ensemble des données personnelles d'un compte (RGPD art. 15 « droit d'accès »
 * et art. 20 « portabilité »), dans un format structuré et lisible par machine.
 *
 * @param shareLink lien de partage actif, ou {@code null} si le compte n'en a pas.
 */
public record PersonalDataExport(
        LocalDateTime exportedAt,
        Account account,
        List<ApplicationDto> applications,
        List<MailScanEntry> mailScans,
        ShareLinkEntry shareLink
) {

    /** Lien de partage en lecture seule, tel qu'exportable au titulaire. */
    public record ShareLinkEntry(
            String token,
            LocalDateTime createdAt,
            LocalDateTime expiresAt
    ) {
        public static ShareLinkEntry from(ShareLink link) {
            return new ShareLinkEntry(link.getToken(), link.getCreatedAt(), link.getExpiresAt());
        }
    }

    /**
     * Les jetons OAuth Google/Gmail sont volontairement absents : ce sont des secrets
     * d'accès, pas des informations sur la personne. Leur seule présence est signalée.
     */
    public record Account(
            UUID id,
            String email,
            String name,
            String avatarUrl,
            boolean hasPassword,
            boolean gmailConnected,
            LocalDateTime lastMailScanAt,
            LocalDateTime createdAt,
            LocalDateTime updatedAt
    ) {
        public static Account from(User u) {
            return new Account(
                    u.getId(),
                    u.getEmail(),
                    u.getName(),
                    u.getAvatarUrl(),
                    u.getPasswordHash() != null,
                    u.getGmailToken() != null,
                    u.getLastMailScanAt(),
                    u.getCreatedAt(),
                    u.getUpdatedAt()
            );
        }
    }

    /** Journal d'un scan Gmail : ce qui a été analysé, jamais le contenu des messages. */
    public record MailScanEntry(
            UUID id,
            LocalDateTime scannedAt,
            Integer mailsAnalyzed,
            Integer matchesFound,
            String status,
            String errorMessage
    ) {
        public static MailScanEntry from(MailScan s) {
            return new MailScanEntry(
                    s.getId(),
                    s.getScannedAt(),
                    s.getMailsAnalyzed(),
                    s.getMatchesFound(),
                    s.getStatus(),
                    s.getErrorMessage()
            );
        }
    }
}
