package com.jobdesk.web;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.jobdesk.domain.MailScan;
import com.jobdesk.domain.User;
import com.jobdesk.repository.ApplicationRepository;
import com.jobdesk.repository.MailScanRepository;
import com.jobdesk.repository.RefreshTokenRepository;
import com.jobdesk.repository.UserRepository;
import com.jobdesk.security.JwtService;
import com.jobdesk.security.RateLimiter;
import com.jobdesk.service.RefreshTokenService;
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
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Droits RGPD : accès/portabilité (art. 15 et 20) et effacement (art. 17).
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class PersonalDataTest {

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
    PasswordEncoder passwordEncoder;
    @Autowired
    JwtService jwtService;
    @Autowired
    RefreshTokenService refreshTokenService;
    @Autowired
    RateLimiter rateLimiter;
    @Autowired
    ObjectMapper objectMapper;

    private User user;
    private String bearer;

    @BeforeEach
    void setUp() {
        // Base H2 partagée : nettoyage dans l'ordre des dépendances.
        refreshTokenRepository.deleteAll();
        mailScanRepository.deleteAll();
        applicationRepository.deleteAll();
        userRepository.deleteAll();
        rateLimiter.clear();

        user = new User();
        user.setEmail("alice@example.com");
        user.setName("Alice");
        user.setPasswordHash(passwordEncoder.encode("correct-horse"));
        user.setGmailToken("un-token-gmail-secret");
        user = userRepository.save(user);
        bearer = "Bearer " + jwtService.generate(user);
    }

    private void givenAnApplication() throws Exception {
        mvc.perform(post("/api/applications").header("Authorization", bearer)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"companyName": "Acme", "jobTitle": "Développeur"}"""))
                .andExpect(status().isCreated());
    }

    private void givenAMailScan() {
        MailScan scan = new MailScan();
        scan.setUser(user);
        scan.setScannedAt(LocalDateTime.now());
        scan.setMailsAnalyzed(12);
        scan.setMatchesFound(2);
        mailScanRepository.save(scan);
    }

    // ─── Accès et portabilité ────────────────────────────────────────────────

    @Test
    void exportContainsTheAccountItsApplicationsAndItsScans() throws Exception {
        givenAnApplication();
        givenAMailScan();

        mvc.perform(get("/api/me/export").header("Authorization", bearer))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.account.email").value("alice@example.com"))
                .andExpect(jsonPath("$.account.name").value("Alice"))
                .andExpect(jsonPath("$.applications[0].companyName").value("Acme"))
                .andExpect(jsonPath("$.mailScans[0].mailsAnalyzed").value(12))
                .andExpect(jsonPath("$.exportedAt").isNotEmpty());
    }

    /** L'export est une pièce jointe : le navigateur doit le télécharger, pas l'afficher. */
    @Test
    void exportIsServedAsADownload() throws Exception {
        mvc.perform(get("/api/me/export").header("Authorization", bearer))
                .andExpect(status().isOk())
                .andExpect(result -> assertThat(result.getResponse().getHeader("Content-Disposition"))
                        .startsWith("attachment;")
                        .contains(".json"));
    }

    /**
     * Les jetons OAuth sont des secrets d'accès, pas des données sur la personne :
     * seule leur présence est indiquée, jamais leur valeur.
     */
    @Test
    void exportSignalsGmailIsConnectedWithoutLeakingTheToken() throws Exception {
        String body = mvc.perform(get("/api/me/export").header("Authorization", bearer))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.account.gmailConnected").value(true))
                .andExpect(jsonPath("$.account.hasPassword").value(true))
                .andReturn().getResponse().getContentAsString();

        assertThat(body).doesNotContain("un-token-gmail-secret");
        assertThat(body).doesNotContain("passwordHash");
    }

    @Test
    void exportRequiresAuthentication() throws Exception {
        mvc.perform(get("/api/me/export")).andExpect(status().isUnauthorized());
    }

    /** Chacun n'exporte que ses propres données. */
    @Test
    void exportNeverIncludesAnotherUsersApplications() throws Exception {
        givenAnApplication();

        User other = new User();
        other.setEmail("bob@example.com");
        other.setName("Bob");
        other = userRepository.save(other);

        mvc.perform(get("/api/me/export").header("Authorization", "Bearer " + jwtService.generate(other)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.applications").isEmpty());
    }

    // ─── Effacement ──────────────────────────────────────────────────────────

    @Test
    void deletingTheAccountRemovesEveryTrace() throws Exception {
        givenAnApplication();
        givenAMailScan();
        refreshTokenService.issue(user);

        mvc.perform(delete("/api/me").header("Authorization", bearer)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"password": "correct-horse"}"""))
                .andExpect(status().isNoContent());

        assertThat(userRepository.findByEmail("alice@example.com")).isEmpty();
        assertThat(applicationRepository.count()).isZero();
        assertThat(mailScanRepository.count()).isZero();
        assertThat(refreshTokenRepository.count()).isZero();
    }

    /** Le JWT reste valide cryptographiquement : c'est l'absence du compte qui doit fermer la porte. */
    @Test
    void theSessionNoLongerWorksOnceTheAccountIsGone() throws Exception {
        mvc.perform(delete("/api/me").header("Authorization", bearer)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"password": "correct-horse"}"""))
                .andExpect(status().isNoContent());

        mvc.perform(get("/api/me").header("Authorization", bearer))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void deletingRefusesAWrongPasswordAndKeepsTheData() throws Exception {
        givenAnApplication();

        mvc.perform(delete("/api/me").header("Authorization", bearer)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"password": "mauvais"}"""))
                .andExpect(status().isForbidden());

        assertThat(userRepository.findByEmail("alice@example.com")).isPresent();
        assertThat(applicationRepository.count()).isEqualTo(1);
    }

    @Test
    void deletingRefusesWhenThePasswordIsMissing() throws Exception {
        mvc.perform(delete("/api/me").header("Authorization", bearer))
                .andExpect(status().isForbidden());

        assertThat(userRepository.findByEmail("alice@example.com")).isPresent();
    }

    /** Un compte Google n'a pas de mot de passe : le JWT suffit à confirmer. */
    @Test
    void aGoogleOnlyAccountCanBeDeletedWithoutAPassword() throws Exception {
        User google = new User();
        google.setEmail("google@example.com");
        google.setName("Google User");
        google = userRepository.save(google);

        mvc.perform(delete("/api/me").header("Authorization", "Bearer " + jwtService.generate(google)))
                .andExpect(status().isNoContent());

        assertThat(userRepository.findByEmail("google@example.com")).isEmpty();
    }

    @Test
    void deletingRequiresAuthentication() throws Exception {
        mvc.perform(delete("/api/me")).andExpect(status().isUnauthorized());
    }

    /** Supprimer son compte ne doit pas emporter celui des autres. */
    @Test
    void deletingLeavesOtherAccountsUntouched() throws Exception {
        givenAnApplication();

        User other = new User();
        other.setEmail("bob@example.com");
        other.setName("Bob");
        other = userRepository.save(other);
        String otherBearer = "Bearer " + jwtService.generate(other);
        mvc.perform(post("/api/applications").header("Authorization", otherBearer)
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {"companyName": "Globex", "jobTitle": "SRE"}"""));

        mvc.perform(delete("/api/me").header("Authorization", bearer)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"password": "correct-horse"}"""))
                .andExpect(status().isNoContent());

        assertThat(userRepository.findByEmail("bob@example.com")).isPresent();
        JsonNode export = objectMapper.readTree(
                mvc.perform(get("/api/me/export").header("Authorization", otherBearer))
                        .andReturn().getResponse().getContentAsString());
        assertThat(export.get("applications")).hasSize(1);
    }
}
