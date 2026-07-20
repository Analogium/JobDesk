package com.jobdesk.domain;

import com.jobdesk.domain.converter.EncryptedStringConverter;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.test.context.ActiveProfiles;

import org.springframework.beans.factory.annotation.Autowired;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Non-régression du wipe de tokens : si la clé de déchiffrement ne correspond pas,
 * charger puis sauvegarder un User (en ne modifiant qu'un autre champ) ne doit PAS
 * écraser le token chiffré en base. Garanti par {@code @DynamicUpdate} sur User.
 */
@DataJpaTest
@ActiveProfiles("test")
class UserTokenPreservationTest {

    private static final String KEY_A = "the-real-secret-used-to-store-token";
    private static final String KEY_B = "a-totally-different-wrong-secret-xx";

    @Autowired
    TestEntityManager em;

    @Test
    void savingUserWithWrongKeyDoesNotWipeEncryptedToken() {
        // Stocké avec la "vraie" clé A
        EncryptedStringConverter.initKey(KEY_A);
        User user = new User();
        user.setEmail("victim@example.com");
        user.setName("Before");
        user.setGmailToken("ya29.precious-token");
        UUID id = em.persistAndGetId(user, UUID.class);
        em.flush();
        em.clear();

        // Chargé + sauvé avec une clé B ERRONÉE : le token décode en null en mémoire,
        // mais on ne modifie QUE le nom. @DynamicUpdate ne doit pas réécrire gmail_token.
        EncryptedStringConverter.initKey(KEY_B);
        User loaded = em.find(User.class, id);
        assertThat(loaded.getGmailToken()).isNull(); // déchiffrement échoue avec la mauvaise clé
        loaded.setName("After");
        em.flush();
        em.clear();

        // Avec la vraie clé A de nouveau, le token doit toujours être là.
        EncryptedStringConverter.initKey(KEY_A);
        User recovered = em.find(User.class, id);
        assertThat(recovered.getName()).isEqualTo("After");
        assertThat(recovered.getGmailToken()).isEqualTo("ya29.precious-token");

        EncryptedStringConverter.initKey("change_me_generate_a_32_char_secret"); // reset pour les autres tests
    }
}
