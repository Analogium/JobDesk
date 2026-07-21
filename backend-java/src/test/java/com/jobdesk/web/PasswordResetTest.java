package com.jobdesk.web;

import com.jobdesk.domain.PasswordResetToken;
import com.jobdesk.domain.User;
import com.jobdesk.repository.ApplicationRepository;
import com.jobdesk.repository.PasswordResetTokenRepository;
import com.jobdesk.repository.UserRepository;
import com.jobdesk.security.RateLimiter;
import com.jobdesk.service.MailService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Parcours « mot de passe oublié » : demande de lien puis réinitialisation.
 * L'envoi de mail est simulé, on vérifie le lien qui aurait été envoyé.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class PasswordResetTest {

    @Autowired
    MockMvc mvc;
    @Autowired
    UserRepository userRepository;
    @Autowired
    ApplicationRepository applicationRepository;
    @Autowired
    PasswordResetTokenRepository tokenRepository;
    @Autowired
    PasswordEncoder passwordEncoder;

    @Autowired
    RateLimiter rateLimiter;

    @MockitoBean
    MailService mailService;

    @BeforeEach
    void setUp() {
        tokenRepository.deleteAll();
        applicationRepository.deleteAll();
        userRepository.deleteAll();
        // Le limiteur est un singleton partagé par toutes les classes de test : sans
        // remise à zéro, les demandes s'accumulent et finissent par être rejetées en 429.
        rateLimiter.clear();
    }

    private User givenUserWithPassword(String email, String password) {
        User user = new User();
        user.setEmail(email);
        user.setName("Alice");
        user.setPasswordHash(passwordEncoder.encode(password));
        return userRepository.save(user);
    }

    private void forgot(String email) throws Exception {
        mvc.perform(post("/auth/password/forgot").contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"email": "%s"}""".formatted(email)))
                .andExpect(status().isNoContent());
    }

    /** Récupère le token depuis le lien passé au service de mail. */
    private String capturedToken(String email) {
        ArgumentCaptor<String> url = ArgumentCaptor.forClass(String.class);
        verify(mailService).sendPasswordReset(eq(email), url.capture());
        return url.getValue().replaceAll(".*token=", "");
    }

    private void reset(String token, String password, int expectedStatus) throws Exception {
        mvc.perform(post("/auth/password/reset").contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"token": "%s", "password": "%s"}""".formatted(token, password)))
                .andExpect(status().is(expectedStatus));
    }

    @Test
    void resetLinkLetsTheUserChooseANewPassword() throws Exception {
        givenUserWithPassword("alice@example.com", "ancien-mot-de-passe");

        forgot("alice@example.com");
        reset(capturedToken("alice@example.com"), "nouveau-mot-de-passe", 204);

        User updated = userRepository.findByEmail("alice@example.com").orElseThrow();
        assertThat(passwordEncoder.matches("nouveau-mot-de-passe", updated.getPasswordHash())).isTrue();
        assertThat(passwordEncoder.matches("ancien-mot-de-passe", updated.getPasswordHash())).isFalse();
    }

    /** Répondre différemment ici révélerait quelles adresses ont un compte. */
    @Test
    void unknownEmailAnswersTheSameAndSendsNothing() throws Exception {
        forgot("personne@example.com");
        verify(mailService, never()).sendPasswordReset(anyString(), anyString());
        assertThat(tokenRepository.count()).isZero();
    }

    /** Le token n'est stocké que sous forme d'empreinte : la base ne permet pas de forger un lien. */
    @Test
    void tokenIsNeverStoredInClearText() throws Exception {
        givenUserWithPassword("alice@example.com", "ancien-mot-de-passe");
        forgot("alice@example.com");

        String token = capturedToken("alice@example.com");
        PasswordResetToken stored = tokenRepository.findAll().getFirst();

        assertThat(stored.getTokenHash()).isNotEqualTo(token);
        assertThat(stored.getTokenHash()).hasSize(64);
    }

    @Test
    void aLinkCannotBeUsedTwice() throws Exception {
        givenUserWithPassword("alice@example.com", "ancien-mot-de-passe");
        forgot("alice@example.com");
        String token = capturedToken("alice@example.com");

        reset(token, "nouveau-mot-de-passe", 204);
        reset(token, "encore-un-autre", 400);

        User updated = userRepository.findByEmail("alice@example.com").orElseThrow();
        assertThat(passwordEncoder.matches("nouveau-mot-de-passe", updated.getPasswordHash())).isTrue();
    }

    @Test
    void anExpiredLinkIsRefused() throws Exception {
        givenUserWithPassword("alice@example.com", "ancien-mot-de-passe");
        forgot("alice@example.com");
        String token = capturedToken("alice@example.com");

        PasswordResetToken stored = tokenRepository.findAll().getFirst();
        stored.setExpiresAt(LocalDateTime.now().minusMinutes(1));
        tokenRepository.save(stored);

        reset(token, "nouveau-mot-de-passe", 400);
    }

    @Test
    void anUnknownTokenIsRefused() throws Exception {
        reset("token-inexistant", "nouveau-mot-de-passe", 400);
    }

    /** Une nouvelle demande doit invalider le lien précédent. */
    @Test
    void requestingAgainInvalidatesThePreviousLink() throws Exception {
        givenUserWithPassword("alice@example.com", "ancien-mot-de-passe");

        forgot("alice@example.com");
        forgot("alice@example.com");

        ArgumentCaptor<String> urls = ArgumentCaptor.forClass(String.class);
        verify(mailService, org.mockito.Mockito.times(2))
                .sendPasswordReset(eq("alice@example.com"), urls.capture());

        String first = urls.getAllValues().get(0).replaceAll(".*token=", "");
        String second = urls.getAllValues().get(1).replaceAll(".*token=", "");

        assertThat(tokenRepository.count()).isEqualTo(1);
        reset(first, "nouveau-mot-de-passe", 400);
        reset(second, "nouveau-mot-de-passe", 204);
    }

    /** Recevoir le mail prouve qu'on possède l'adresse : un compte Google peut donc ajouter un mot de passe. */
    @Test
    void aGoogleOnlyAccountCanSetAPasswordThroughTheResetFlow() throws Exception {
        User google = new User();
        google.setEmail("google@example.com");
        google.setName("Google User");
        userRepository.save(google);

        forgot("google@example.com");
        reset(capturedToken("google@example.com"), "mon-nouveau-mdp", 204);

        User updated = userRepository.findByEmail("google@example.com").orElseThrow();
        assertThat(passwordEncoder.matches("mon-nouveau-mdp", updated.getPasswordHash())).isTrue();
    }

    /**
     * Sans plafond, rejouer la demande en boucle viderait le quota d'envoi Brevo
     * et inonderait la boîte de la victime.
     */
    @Test
    void repeatedRequestsForTheSameAddressAreThrottled() throws Exception {
        givenUserWithPassword("alice@example.com", "ancien-mot-de-passe");

        forgot("alice@example.com");
        forgot("alice@example.com");
        forgot("alice@example.com");

        mvc.perform(post("/auth/password/forgot").contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"email": "alice@example.com"}"""))
                .andExpect(status().isTooManyRequests());

        // Le 4e appel ne doit avoir déclenché aucun envoi supplémentaire.
        verify(mailService, org.mockito.Mockito.times(3))
                .sendPasswordReset(eq("alice@example.com"), anyString());
    }

    /** Le plafond s'applique avant la recherche en base : une adresse inconnue est limitée pareil. */
    @Test
    void throttlingDoesNotRevealWhetherTheAddressExists() throws Exception {
        for (int i = 0; i < 3; i++) {
            forgot("inconnu@example.com");
        }

        mvc.perform(post("/auth/password/forgot").contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"email": "inconnu@example.com"}"""))
                .andExpect(status().isTooManyRequests());
    }

    @Test
    void aTooShortPasswordIsRefused() throws Exception {
        givenUserWithPassword("alice@example.com", "ancien-mot-de-passe");
        forgot("alice@example.com");

        reset(capturedToken("alice@example.com"), "court", 400);
    }
}
