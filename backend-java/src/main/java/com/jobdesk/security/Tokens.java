package com.jobdesk.security;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.Base64;
import java.util.HexFormat;

/**
 * Génération et hachage des jetons opaques (réinitialisation de mot de passe, refresh).
 *
 * <p>Ces jetons sont tirés d'un {@link SecureRandom} et ne sont stockés en base que sous
 * forme d'empreinte SHA-256 : la valeur en clair n'existe que le temps d'être remise au
 * client, l'empreinte ne permet pas de la reconstruire.
 */
public final class Tokens {

    private static final SecureRandom RANDOM = new SecureRandom();
    private static final int TOKEN_BYTES = 32;

    private Tokens() {
    }

    /** Jeton aléatoire de 32 octets, encodé en base64 URL-safe. */
    public static String random() {
        byte[] raw = new byte[TOKEN_BYTES];
        RANDOM.nextBytes(raw);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(raw);
    }

    /** Empreinte hexadécimale SHA-256 (64 caractères). */
    public static String sha256(String value) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            return HexFormat.of().formatHex(digest.digest(value.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 indisponible", e);
        }
    }
}
