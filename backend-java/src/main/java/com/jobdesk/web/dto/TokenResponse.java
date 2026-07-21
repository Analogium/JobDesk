package com.jobdesk.web.dto;

/**
 * Même jeton que celui délivré par le flux Google : le frontend le stocke et
 * l'envoie en {@code Authorization: Bearer}, sans distinction de provenance.
 */
public record TokenResponse(String token, UserDto user) {
}
