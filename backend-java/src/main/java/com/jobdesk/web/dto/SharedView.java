package com.jobdesk.web.dto;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Réponse publique d'un lien de partage : les candidatures du titulaire en lecture seule.
 *
 * @param ownerName nom du titulaire (peut être {@code null} : le front affiche alors un
 *                  libellé générique).
 * @param expiresAt échéance du lien, {@code null} s'il n'expire jamais.
 */
public record SharedView(
        String ownerName,
        LocalDateTime expiresAt,
        List<SharedApplicationDto> applications
) {
}
