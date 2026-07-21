package com.jobdesk.security;

import jakarta.servlet.http.HttpServletRequest;

/**
 * Adresse IP réelle de l'appelant. La chaîne en production est
 * {@code client → Cloudflare → Traefik → application}.
 */
public final class ClientIp {

    private ClientIp() {
    }

    public static String of(HttpServletRequest request) {
        // Cloudflare renseigne CF-Connecting-IP avec l'IP réelle du client et écrase toute
        // valeur envoyée par ce dernier. C'est la seule source fiable ici : la DERNIÈRE
        // entrée de X-Forwarded-For est l'edge Cloudflare, qui change d'une requête à
        // l'autre — s'en servir rendrait tout comptage par IP inopérant.
        String cloudflare = request.getHeader("CF-Connecting-IP");
        if (cloudflare != null && !cloudflare.isBlank()) {
            return cloudflare.trim();
        }

        // Sans Cloudflare (dev local, appel direct à l'origine) : dernière entrée de
        // X-Forwarded-For, celle ajoutée par le proxy le plus proche, donc la seule que
        // le client ne peut pas falsifier en envoyant son propre en-tête.
        String forwarded = request.getHeader("X-Forwarded-For");
        if (forwarded == null || forwarded.isBlank()) {
            return request.getRemoteAddr();
        }
        String[] parts = forwarded.split(",");
        return parts[parts.length - 1].trim();
    }
}
