package com.jobdesk.domain.converter;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.util.Arrays;
import java.util.Base64;
import javax.crypto.Cipher;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;

/**
 * Chiffre/déchiffre des chaînes en AES-256-GCM, format compatible bit-à-bit avec
 * l'ancien backend Symfony ({@code App\Doctrine\Type\EncryptedStringType}).
 *
 * <p>Format sur disque : {@code base64( iv[12] || tag[16] || ciphertext )}.
 * Clé : {@code SHA-256(APP_SECRET)} (32 octets). La clé est injectée une fois au
 * démarrage par {@link com.jobdesk.config.EncryptionKeyInitializer} (Hibernate
 * instancie ce converter hors du contexte Spring, d'où le holder statique).
 */
@Converter(autoApply = false)
public class EncryptedStringConverter implements AttributeConverter<String, String> {

    private static final String TRANSFORMATION = "AES/GCM/NoPadding";
    private static final int IV_LENGTH = 12;
    private static final int TAG_LENGTH = 16; // octets
    private static final int TAG_BITS = TAG_LENGTH * 8;

    private static volatile SecretKeySpec key;
    private static final SecureRandom RANDOM = new SecureRandom();

    /** Dérive une clé 32 octets depuis APP_SECRET, comme {@code hash('sha256', ..., binary: true)}. */
    public static void initKey(String rawSecret) {
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256")
                    .digest(rawSecret.getBytes(StandardCharsets.UTF_8));
            key = new SecretKeySpec(digest, "AES");
        } catch (Exception e) {
            throw new IllegalStateException("Impossible de dériver la clé de chiffrement", e);
        }
    }

    @Override
    public String convertToDatabaseColumn(String value) {
        if (value == null || value.isEmpty()) {
            return null;
        }
        requireKey();
        try {
            byte[] iv = new byte[IV_LENGTH];
            RANDOM.nextBytes(iv);

            Cipher cipher = Cipher.getInstance(TRANSFORMATION);
            cipher.init(Cipher.ENCRYPT_MODE, key, new GCMParameterSpec(TAG_BITS, iv));
            // doFinal renvoie ciphertext || tag (tag sur les 16 derniers octets)
            byte[] out = cipher.doFinal(value.getBytes(StandardCharsets.UTF_8));

            int cipherLen = out.length - TAG_LENGTH;
            byte[] ciphertext = Arrays.copyOfRange(out, 0, cipherLen);
            byte[] tag = Arrays.copyOfRange(out, cipherLen, out.length);

            // Réordonne en iv || tag || ciphertext (format PHP)
            byte[] combined = new byte[IV_LENGTH + TAG_LENGTH + ciphertext.length];
            System.arraycopy(iv, 0, combined, 0, IV_LENGTH);
            System.arraycopy(tag, 0, combined, IV_LENGTH, TAG_LENGTH);
            System.arraycopy(ciphertext, 0, combined, IV_LENGTH + TAG_LENGTH, ciphertext.length);

            return Base64.getEncoder().encodeToString(combined);
        } catch (Exception e) {
            throw new IllegalStateException("Échec du chiffrement", e);
        }
    }

    @Override
    public String convertToEntityAttribute(String dbData) {
        if (dbData == null || dbData.isEmpty()) {
            return null;
        }
        requireKey();
        try {
            byte[] decoded = Base64.getDecoder().decode(dbData);
            if (decoded.length <= IV_LENGTH + TAG_LENGTH) {
                return null;
            }
            byte[] iv = Arrays.copyOfRange(decoded, 0, IV_LENGTH);
            byte[] tag = Arrays.copyOfRange(decoded, IV_LENGTH, IV_LENGTH + TAG_LENGTH);
            byte[] ciphertext = Arrays.copyOfRange(decoded, IV_LENGTH + TAG_LENGTH, decoded.length);

            // Java attend ciphertext || tag pour doFinal
            byte[] combined = new byte[ciphertext.length + TAG_LENGTH];
            System.arraycopy(ciphertext, 0, combined, 0, ciphertext.length);
            System.arraycopy(tag, 0, combined, ciphertext.length, TAG_LENGTH);

            Cipher cipher = Cipher.getInstance(TRANSFORMATION);
            cipher.init(Cipher.DECRYPT_MODE, key, new GCMParameterSpec(TAG_BITS, iv));
            byte[] plain = cipher.doFinal(combined);
            return new String(plain, StandardCharsets.UTF_8);
        } catch (Exception e) {
            // Comme le backend PHP : un déchiffrement invalide renvoie null plutôt que de planter.
            return null;
        }
    }

    private static void requireKey() {
        if (key == null) {
            throw new IllegalStateException("EncryptedStringConverter: clé non initialisée.");
        }
    }
}
