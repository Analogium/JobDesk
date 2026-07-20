package com.jobdesk.domain.converter;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class EncryptedStringConverterTest {

    private static final String SECRET = "change_me_generate_a_32_char_secret";
    private final EncryptedStringConverter converter = new EncryptedStringConverter();

    @BeforeAll
    static void initKey() {
        EncryptedStringConverter.initKey(SECRET);
    }

    @Test
    void roundTrip() {
        String plain = "ya29.some-secret-gmail-token";
        String encrypted = converter.convertToDatabaseColumn(plain);

        assertThat(encrypted).isNotNull().isNotEqualTo(plain);
        assertThat(converter.convertToEntityAttribute(encrypted)).isEqualTo(plain);
    }

    @Test
    void nullAndEmptyMapToNull() {
        assertThat(converter.convertToDatabaseColumn(null)).isNull();
        assertThat(converter.convertToDatabaseColumn("")).isNull();
        assertThat(converter.convertToEntityAttribute(null)).isNull();
        assertThat(converter.convertToEntityAttribute("")).isNull();
    }

    @Test
    void eachEncryptionUsesFreshIv() {
        String plain = "same-input";
        assertThat(converter.convertToDatabaseColumn(plain))
                .isNotEqualTo(converter.convertToDatabaseColumn(plain));
    }

    /**
     * Interopérabilité : un ciphertext produit par l'ancien backend PHP
     * ({@code EncryptedStringType}, AES-256-GCM, même clé) doit être déchiffrable ici.
     * Vecteur généré via openssl PHP avec le secret ci-dessus.
     */
    @Test
    void decryptsPhpProducedCiphertext() {
        String phpEncrypted =
                "9w9TgaISwmtocAj8dlzg6Q7y3zqM+/V3+X3m2syLLQNmMFmb8ZP4vFGr3XnfAek4LxMtGRgFQNmz+0kdlusPgw==";
        assertThat(converter.convertToEntityAttribute(phpEncrypted))
                .isEqualTo("ya29.demo-gmail-access-token-EXAMPLE");
    }
}
