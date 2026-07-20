package com.jobdesk.config;

import com.jobdesk.domain.converter.EncryptedStringConverter;
import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * Initialise la clé de chiffrement AES du {@link EncryptedStringConverter} au démarrage,
 * équivalent de l'ancien {@code EncryptionKeySubscriber} Symfony.
 */
@Component
public class EncryptionKeyInitializer {

    public EncryptionKeyInitializer(@Value("${app.encryption-secret}") String secret) {
        // Init dès la construction pour couvrir toute opération JPA précoce.
        EncryptedStringConverter.initKey(secret);
    }

    @PostConstruct
    void ready() {
        // no-op : la clé est déjà posée dans le constructeur.
    }
}
