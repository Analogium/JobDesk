package com.jobdesk.web;

import com.jobdesk.domain.User;
import com.jobdesk.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Inscription / connexion par email + mot de passe, et cohabitation avec les comptes Google.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class PasswordAuthTest {

    @Autowired
    MockMvc mvc;
    @Autowired
    UserRepository userRepository;
    @Autowired
    PasswordEncoder passwordEncoder;

    @BeforeEach
    void setUp() {
        userRepository.deleteAll();
    }

    private String json(String email, String name, String password) {
        return name == null
                ? """
                {"email": "%s", "password": "%s"}""".formatted(email, password)
                : """
                {"email": "%s", "name": "%s", "password": "%s"}""".formatted(email, name, password);
    }

    @Test
    void registerReturnsTokenAndPersistsHashedPassword() throws Exception {
        mvc.perform(post("/auth/register").contentType(MediaType.APPLICATION_JSON)
                        .content(json("alice@example.com", "Alice", "correct-horse")))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.token").isNotEmpty())
                .andExpect(jsonPath("$.user.email").value("alice@example.com"));

        User saved = userRepository.findByEmail("alice@example.com").orElseThrow();
        assertThat(saved.getPasswordHash()).isNotNull();
        // Le mot de passe ne doit jamais être stocké en clair.
        assertThat(saved.getPasswordHash()).doesNotContain("correct-horse");
        assertThat(passwordEncoder.matches("correct-horse", saved.getPasswordHash())).isTrue();
    }

    @Test
    void registeredUserCanUseTheTokenOnTheApi() throws Exception {
        String body = mvc.perform(post("/auth/register").contentType(MediaType.APPLICATION_JSON)
                        .content(json("bob@example.com", "Bob", "correct-horse")))
                .andReturn().getResponse().getContentAsString();
        String token = body.replaceAll(".*\"token\"\\s*:\\s*\"([^\"]+)\".*", "$1");

        mvc.perform(get("/api/me").header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email").value("bob@example.com"));
    }

    @Test
    void emailIsNormalisedSoTheSameAddressCannotBeRegisteredTwice() throws Exception {
        mvc.perform(post("/auth/register").contentType(MediaType.APPLICATION_JSON)
                        .content(json("Alice@Example.COM", "Alice", "correct-horse")))
                .andExpect(status().isCreated());

        assertThat(userRepository.findByEmail("alice@example.com")).isPresent();

        mvc.perform(post("/auth/register").contentType(MediaType.APPLICATION_JSON)
                        .content(json("alice@example.com", "Alice", "another-pass")))
                .andExpect(status().isConflict());
    }

    @Test
    void registerRejectsAnEmailAlreadyUsedByAGoogleAccount() throws Exception {
        User google = new User();
        google.setEmail("google@example.com");
        google.setName("Google User");
        userRepository.save(google);

        mvc.perform(post("/auth/register").contentType(MediaType.APPLICATION_JSON)
                        .content(json("google@example.com", "Impostor", "correct-horse")))
                .andExpect(status().isConflict());
    }

    @Test
    void loginSucceedsWithTheRightPassword() throws Exception {
        mvc.perform(post("/auth/register").contentType(MediaType.APPLICATION_JSON)
                .content(json("alice@example.com", "Alice", "correct-horse")));

        mvc.perform(post("/auth/login").contentType(MediaType.APPLICATION_JSON)
                        .content(json("alice@example.com", null, "correct-horse")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").isNotEmpty())
                .andExpect(jsonPath("$.user.name").value("Alice"));
    }

    @Test
    void loginFailsWithTheWrongPassword() throws Exception {
        mvc.perform(post("/auth/register").contentType(MediaType.APPLICATION_JSON)
                .content(json("alice@example.com", "Alice", "correct-horse")));

        mvc.perform(post("/auth/login").contentType(MediaType.APPLICATION_JSON)
                        .content(json("alice@example.com", null, "wrong-password")))
                .andExpect(status().isUnauthorized());
    }

    /**
     * Le message doit être identique dans les deux cas, sinon il permet de savoir
     * quelles adresses ont un compte.
     */
    @Test
    void unknownEmailAndWrongPasswordAreIndistinguishable() throws Exception {
        mvc.perform(post("/auth/register").contentType(MediaType.APPLICATION_JSON)
                .content(json("alice@example.com", "Alice", "correct-horse")));

        String wrongPassword = mvc.perform(post("/auth/login").contentType(MediaType.APPLICATION_JSON)
                        .content(json("alice@example.com", null, "wrong-password")))
                .andExpect(status().isUnauthorized())
                .andReturn().getResponse().getContentAsString();

        String unknownEmail = mvc.perform(post("/auth/login").contentType(MediaType.APPLICATION_JSON)
                        .content(json("nobody@example.com", null, "wrong-password")))
                .andExpect(status().isUnauthorized())
                .andReturn().getResponse().getContentAsString();

        assertThat(wrongPassword).isEqualTo(unknownEmail);
    }

    /** Un compte Google n'a pas de mot de passe : aucun mot de passe ne doit ouvrir la porte. */
    @Test
    void googleOnlyAccountCannotBeLoggedIntoWithAPassword() throws Exception {
        User google = new User();
        google.setEmail("google@example.com");
        google.setName("Google User");
        userRepository.save(google);

        mvc.perform(post("/auth/login").contentType(MediaType.APPLICATION_JSON)
                        .content(json("google@example.com", null, "")))
                .andExpect(status().isBadRequest());

        mvc.perform(post("/auth/login").contentType(MediaType.APPLICATION_JSON)
                        .content(json("google@example.com", null, "any-password")))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void registerRejectsAShortPasswordAndAnInvalidEmail() throws Exception {
        mvc.perform(post("/auth/register").contentType(MediaType.APPLICATION_JSON)
                        .content(json("alice@example.com", "Alice", "court")))
                .andExpect(status().isBadRequest());

        mvc.perform(post("/auth/register").contentType(MediaType.APPLICATION_JSON)
                        .content(json("pas-un-email", "Alice", "correct-horse")))
                .andExpect(status().isBadRequest());

        assertThat(userRepository.count()).isZero();
    }
}
