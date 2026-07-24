package com.jobdesk.web;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.jobdesk.domain.ShareLink;
import com.jobdesk.domain.User;
import com.jobdesk.repository.ApplicationRepository;
import com.jobdesk.repository.MailScanRepository;
import com.jobdesk.repository.RefreshTokenRepository;
import com.jobdesk.repository.ShareLinkRepository;
import com.jobdesk.repository.UserRepository;
import com.jobdesk.security.JwtService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.nullValue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Lien de partage en lecture seule : génération/révocation par le titulaire, accès
 * public réduit par un tiers, unicité du lien et expiration.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class ShareLinkIntegrationTest {

    @Autowired
    MockMvc mvc;
    @Autowired
    UserRepository userRepository;
    @Autowired
    ApplicationRepository applicationRepository;
    @Autowired
    MailScanRepository mailScanRepository;
    @Autowired
    RefreshTokenRepository refreshTokenRepository;
    @Autowired
    ShareLinkRepository shareLinkRepository;
    @Autowired
    PasswordEncoder passwordEncoder;
    @Autowired
    JwtService jwtService;
    @Autowired
    ObjectMapper objectMapper;

    private User user;
    private String bearer;

    @BeforeEach
    void setUp() {
        // Base H2 partagée : nettoyage dans l'ordre des dépendances (enfants avant user).
        refreshTokenRepository.deleteAll();
        mailScanRepository.deleteAll();
        shareLinkRepository.deleteAll();
        applicationRepository.deleteAll();
        userRepository.deleteAll();

        user = new User();
        user.setEmail("alice@example.com");
        user.setName("Alice");
        user.setPasswordHash(passwordEncoder.encode("correct-horse"));
        user = userRepository.save(user);
        bearer = "Bearer " + jwtService.generate(user);
    }

    private void givenAnApplicationWithNotes() throws Exception {
        mvc.perform(post("/api/applications").header("Authorization", bearer)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"companyName": "Acme", "jobTitle": "Développeur",
                                 "location": "Lyon", "notes": "NOTE-TRES-PRIVEE"}"""))
                .andExpect(status().isCreated());
    }

    private String generateLink(String jsonBody) throws Exception {
        String body = mvc.perform(post("/api/me/share-link").header("Authorization", bearer)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(jsonBody))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        return objectMapper.readTree(body).get("token").asText();
    }

    // ─── Génération ──────────────────────────────────────────────────────────

    @Test
    void generatesALinkWithAnExpiry() throws Exception {
        mvc.perform(post("/api/me/share-link").header("Authorization", bearer)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"ttlDays": 30}"""))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.token").isNotEmpty())
                .andExpect(jsonPath("$.expiresAt").isNotEmpty());
    }

    @Test
    void generatesAPermanentLinkWhenTtlIsNull() throws Exception {
        mvc.perform(post("/api/me/share-link").header("Authorization", bearer)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"ttlDays": null}"""))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.token").isNotEmpty())
                .andExpect(jsonPath("$.expiresAt").value(nullValue()));
    }

    @Test
    void rejectsAnAbsurdDuration() throws Exception {
        mvc.perform(post("/api/me/share-link").header("Authorization", bearer)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"ttlDays": 1000}"""))
                .andExpect(status().isBadRequest());
    }

    // ─── Lien courant ──────────────────────────────────────────────────────────

    @Test
    void currentReturns204WhenNoLinkExists() throws Exception {
        mvc.perform(get("/api/me/share-link").header("Authorization", bearer))
                .andExpect(status().isNoContent());
    }

    @Test
    void currentReturnsTheActiveLink() throws Exception {
        String token = generateLink("{\"ttlDays\": 7}");

        mvc.perform(get("/api/me/share-link").header("Authorization", bearer))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").value(token));
    }

    // ─── Accès public réduit ────────────────────────────────────────────────

    @Test
    void sharedViewIsPublicAndOmitsNotesAndContacts() throws Exception {
        givenAnApplicationWithNotes();
        String token = generateLink("{\"ttlDays\": 30}");

        // Pas d'en-tête Authorization : l'accès est public.
        String body = mvc.perform(get("/api/shared/" + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.ownerName").value("Alice"))
                .andExpect(jsonPath("$.applications[0].companyName").value("Acme"))
                .andExpect(jsonPath("$.applications[0].location").value("Lyon"))
                .andExpect(jsonPath("$.applications[0].notes").doesNotExist())
                .andExpect(jsonPath("$.applications[0].contacts").doesNotExist())
                .andReturn().getResponse().getContentAsString();

        assertThat(body).doesNotContain("NOTE-TRES-PRIVEE");
    }

    @Test
    void onlyOneLinkIsActiveAtATime() throws Exception {
        String first = generateLink("{\"ttlDays\": 30}");
        String second = generateLink("{\"ttlDays\": 30}");

        assertThat(first).isNotEqualTo(second);
        mvc.perform(get("/api/shared/" + first)).andExpect(status().isNotFound());
        mvc.perform(get("/api/shared/" + second)).andExpect(status().isOk());
        assertThat(shareLinkRepository.count()).isEqualTo(1);
    }

    @Test
    void unknownTokenReturns404() throws Exception {
        mvc.perform(get("/api/shared/does-not-exist")).andExpect(status().isNotFound());
    }

    @Test
    void expiredLinkReturns404() throws Exception {
        ShareLink expired = new ShareLink();
        expired.setUser(user);
        expired.setToken("expired-token-value");
        expired.setExpiresAt(LocalDateTime.now().minusDays(1));
        shareLinkRepository.save(expired);

        mvc.perform(get("/api/shared/expired-token-value")).andExpect(status().isNotFound());
    }

    // ─── Révocation ──────────────────────────────────────────────────────────

    @Test
    void revokedLinkNoLongerResolves() throws Exception {
        String token = generateLink("{\"ttlDays\": 30}");

        mvc.perform(delete("/api/me/share-link").header("Authorization", bearer))
                .andExpect(status().isNoContent());

        mvc.perform(get("/api/shared/" + token)).andExpect(status().isNotFound());
        mvc.perform(get("/api/me/share-link").header("Authorization", bearer))
                .andExpect(status().isNoContent());
    }

    // ─── Effacement du compte ──────────────────────────────────────────────

    @Test
    void deletingTheAccountRemovesTheShareLink() throws Exception {
        String token = generateLink("{\"ttlDays\": 30}");

        mvc.perform(delete("/api/me").header("Authorization", bearer)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"password": "correct-horse"}"""))
                .andExpect(status().isNoContent());

        assertThat(shareLinkRepository.count()).isZero();
        mvc.perform(get("/api/shared/" + token)).andExpect(status().isNotFound());
    }

    // ─── Sécurité ──────────────────────────────────────────────────────────

    @Test
    void managingTheLinkRequiresAuthentication() throws Exception {
        mvc.perform(post("/api/me/share-link")).andExpect(status().isUnauthorized());
        mvc.perform(get("/api/me/share-link")).andExpect(status().isUnauthorized());
        mvc.perform(delete("/api/me/share-link")).andExpect(status().isUnauthorized());
    }

    /** L'export RGPD inclut le lien de partage actif. */
    @Test
    void exportIncludesTheActiveShareLink() throws Exception {
        String token = generateLink("{\"ttlDays\": 30}");

        JsonNode export = objectMapper.readTree(
                mvc.perform(get("/api/me/export").header("Authorization", bearer))
                        .andExpect(status().isOk())
                        .andReturn().getResponse().getContentAsString());

        assertThat(export.get("shareLink").get("token").asText()).isEqualTo(token);
    }
}
