package com.jobdesk.web;

import com.jobdesk.repository.ApplicationRepository;
import com.jobdesk.repository.RefreshTokenRepository;
import com.jobdesk.repository.UserRepository;
import com.jobdesk.security.RateLimiter;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Maintien de session par refresh token : rotation, rejeu, déconnexion.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class RefreshFlowTest {

    @Autowired
    MockMvc mvc;
    @Autowired
    UserRepository userRepository;
    @Autowired
    ApplicationRepository applicationRepository;
    @Autowired
    RefreshTokenRepository refreshTokenRepository;
    @Autowired
    RateLimiter rateLimiter;
    @Autowired
    ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        refreshTokenRepository.deleteAll();
        applicationRepository.deleteAll();
        userRepository.deleteAll();
        rateLimiter.clear();
    }

    /** @return la réponse d'inscription parsée ({token, refreshToken, user}). */
    private JsonNode register(String email) throws Exception {
        MvcResult res = mvc.perform(post("/auth/register").contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"email": "%s", "name": "Alice", "password": "correct-horse"}""".formatted(email)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.refreshToken").isNotEmpty())
                .andReturn();
        return objectMapper.readTree(res.getResponse().getContentAsString());
    }

    private MvcResult refresh(String refreshToken) throws Exception {
        return mvc.perform(post("/auth/refresh").contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"refreshToken": "%s"}""".formatted(refreshToken)))
                .andReturn();
    }

    @Test
    void registerReturnsBothTokensAndTheAccessTokenWorks() throws Exception {
        JsonNode body = register("alice@example.com");

        mvc.perform(get("/api/me").header("Authorization", "Bearer " + body.get("token").asText()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email").value("alice@example.com"));
    }

    @Test
    void refreshReturnsAFreshPairAndTheNewAccessTokenWorks() throws Exception {
        JsonNode body = register("alice@example.com");
        String firstRefresh = body.get("refreshToken").asText();

        MvcResult res = refresh(firstRefresh);
        assertThat(res.getResponse().getStatus()).isEqualTo(200);
        JsonNode refreshed = objectMapper.readTree(res.getResponse().getContentAsString());

        // Le refresh token a tourné : la nouvelle valeur diffère de l'ancienne.
        assertThat(refreshed.get("refreshToken").asText()).isNotEqualTo(firstRefresh);

        mvc.perform(get("/api/me").header("Authorization", "Bearer " + refreshed.get("token").asText()))
                .andExpect(status().isOk());
    }

    @Test
    void aRefreshTokenCannotBeUsedTwice() throws Exception {
        String refreshToken = register("alice@example.com").get("refreshToken").asText();

        assertThat(refresh(refreshToken).getResponse().getStatus()).isEqualTo(200);
        // Rejeu du même token : refusé.
        assertThat(refresh(refreshToken).getResponse().getStatus()).isEqualTo(401);
    }

    /**
     * Rejouer un token déjà consommé signale un vol : toutes les sessions du compte
     * sont révoquées, y compris le token légitimement obtenu à la rotation.
     */
    @Test
    void replayingAConsumedTokenRevokesEverySession() throws Exception {
        String first = register("alice@example.com").get("refreshToken").asText();

        String rotated = objectMapper.readTree(refresh(first).getResponse().getContentAsString())
                .get("refreshToken").asText();

        // On rejoue le premier (déjà consommé) → déclenche la révocation globale.
        assertThat(refresh(first).getResponse().getStatus()).isEqualTo(401);

        // Le token légitime issu de la rotation ne fonctionne plus non plus.
        assertThat(refresh(rotated).getResponse().getStatus()).isEqualTo(401);
    }

    @Test
    void logoutRevokesTheRefreshToken() throws Exception {
        String refreshToken = register("alice@example.com").get("refreshToken").asText();

        mvc.perform(post("/auth/logout").contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"refreshToken": "%s"}""".formatted(refreshToken)))
                .andExpect(status().isNoContent());

        assertThat(refresh(refreshToken).getResponse().getStatus()).isEqualTo(401);
    }

    @Test
    void anUnknownRefreshTokenIsRejected() throws Exception {
        assertThat(refresh("token-inexistant").getResponse().getStatus()).isEqualTo(401);
    }
}
