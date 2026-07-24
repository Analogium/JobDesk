package com.jobdesk.web.dto;

import com.jobdesk.domain.ShareLink;

import java.time.LocalDateTime;

/**
 * Vue destinée au titulaire (Paramètres). Contient le token en clair pour qu'il puisse
 * reconstruire et recopier l'URL de partage à tout moment.
 *
 * @param expiresAt {@code null} si le lien n'expire jamais.
 */
public record ShareLinkDto(
        String token,
        LocalDateTime createdAt,
        LocalDateTime expiresAt
) {
    public static ShareLinkDto from(ShareLink link) {
        return new ShareLinkDto(link.getToken(), link.getCreatedAt(), link.getExpiresAt());
    }
}
