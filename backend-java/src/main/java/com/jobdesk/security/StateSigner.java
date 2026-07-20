package com.jobdesk.security;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.Base64;
import java.util.HexFormat;

/**
 * Signe/vérifie le paramètre OAuth {@code state} en HMAC-SHA256, format {@code payload.sig}
 * (payload = base64(valeur), sig = HMAC hex). Portage de l'ancien signState/verifyState
 * du {@code GmailController} Symfony, avec la même clé (APP_SECRET).
 */
@Component
public class StateSigner {

    private final byte[] key;

    public StateSigner(@Value("${app.encryption-secret}") String secret) {
        this.key = secret.getBytes(StandardCharsets.UTF_8);
    }

    public String sign(String value) {
        String payload = Base64.getEncoder().encodeToString(value.getBytes(StandardCharsets.UTF_8));
        return payload + "." + hmacHex(payload);
    }

    /** @return la valeur d'origine si la signature est valide, sinon null. */
    public String verify(String state) {
        if (state == null) {
            return null;
        }
        int dot = state.indexOf('.');
        if (dot < 0) {
            return null;
        }
        String payload = state.substring(0, dot);
        String sig = state.substring(dot + 1);
        String expected = hmacHex(payload);
        if (!MessageDigest.isEqual(
                expected.getBytes(StandardCharsets.UTF_8), sig.getBytes(StandardCharsets.UTF_8))) {
            return null;
        }
        try {
            return new String(Base64.getDecoder().decode(payload), StandardCharsets.UTF_8);
        } catch (IllegalArgumentException e) {
            return null;
        }
    }

    private String hmacHex(String payload) {
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(key, "HmacSHA256"));
            return HexFormat.of().formatHex(mac.doFinal(payload.getBytes(StandardCharsets.UTF_8)));
        } catch (Exception e) {
            throw new IllegalStateException("HMAC state signing failed", e);
        }
    }
}
