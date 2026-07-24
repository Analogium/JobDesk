package com.jobdesk.web.dto;

/**
 * Demande de génération d'un lien de partage.
 *
 * @param ttlDays durée de validité en jours ; {@code null} = le lien n'expire jamais.
 */
public record CreateShareLinkRequest(Integer ttlDays) {
}
