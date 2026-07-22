package com.jobdesk.web.dto;

/**
 * Réponse d'authentification. {@code token} est l'access token (JWT court) envoyé ensuite
 * en {@code Authorization: Bearer} ; {@code refreshToken} sert à en obtenir un nouveau via
 * {@code /auth/refresh} sans se reconnecter. Le frontend stocke les deux à l'identique,
 * quelle que soit la provenance (email/mot de passe ou Google).
 */
public record TokenResponse(String token, String refreshToken, UserDto user) {
}
