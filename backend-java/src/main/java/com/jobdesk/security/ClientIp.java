package com.jobdesk.security;

import jakarta.servlet.http.HttpServletRequest;

/**
 * Adresse IP réelle de l'appelant, l'application tournant derrière Traefik.
 */
public final class ClientIp {

    private ClientIp() {
    }

    public static String of(HttpServletRequest request) {
        String forwarded = request.getHeader("X-Forwarded-For");
        if (forwarded == null || forwarded.isBlank()) {
            return request.getRemoteAddr();
        }
        // On prend la DERNIÈRE entrée, pas la première : chaque proxy ajoute à droite
        // l'adresse qu'il a vue. La dernière est donc celle constatée par Traefik, la
        // seule non falsifiable — un client peut envoyer son propre X-Forwarded-For et
        // ainsi contrôler entièrement les valeurs de gauche.
        String[] parts = forwarded.split(",");
        return parts[parts.length - 1].trim();
    }
}
