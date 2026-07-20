package com.jobdesk.web;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.jobdesk.domain.User;
import com.jobdesk.repository.UserRepository;
import com.jobdesk.security.JwtService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class ApplicationIntegrationTest {

    @Autowired
    MockMvc mvc;
    @Autowired
    UserRepository userRepository;
    @Autowired
    JwtService jwtService;
    @Autowired
    ObjectMapper objectMapper;

    private String token;

    @BeforeEach
    void setUp() {
        userRepository.deleteAll();
        User user = new User();
        user.setEmail("alice@example.com");
        user.setName("Alice");
        user = userRepository.save(user);
        token = "Bearer " + jwtService.generate(user);
    }

    private String bearerFor(String email, String name) {
        User u = new User();
        u.setEmail(email);
        u.setName(name);
        u = userRepository.save(u);
        return "Bearer " + jwtService.generate(u);
    }

    @Test
    void requiresAuth() throws Exception {
        mvc.perform(get("/api/applications")).andExpect(status().isUnauthorized());
    }

    @Test
    void createListGetPatchDelete() throws Exception {
        // CREATE
        String created = mvc.perform(post("/api/applications")
                        .header("Authorization", token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"companyName":"ACME","jobTitle":"Dev","source":"linkedin","appliedAt":"2026-07-20"}
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.companyName").value("ACME"))
                .andExpect(jsonPath("$.source").value("linkedin"))
                .andExpect(jsonPath("$.status").value("DRAFT"))
                .andReturn().getResponse().getContentAsString();

        String id = objectMapper.readTree(created).get("id").asText();

        // LIST — contrat member / totalItems
        mvc.perform(get("/api/applications?order[createdAt]=desc&page=1")
                        .header("Authorization", token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalItems").value(1))
                .andExpect(jsonPath("$.member[0].id").value(id));

        // PATCH statut → crée un historique
        mvc.perform(patch("/api/applications/" + id)
                        .header("Authorization", token)
                        .contentType("application/merge-patch+json")
                        .content("{\"status\":\"APPLIED\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("APPLIED"))
                .andExpect(jsonPath("$.statusHistories.length()").value(1))
                .andExpect(jsonPath("$.statusHistories[0].previousStatus").value("DRAFT"))
                .andExpect(jsonPath("$.statusHistories[0].newStatus").value("APPLIED"))
                .andExpect(jsonPath("$.statusHistories[0].trigger").value("manual"));

        // DELETE
        mvc.perform(delete("/api/applications/" + id).header("Authorization", token))
                .andExpect(status().isNoContent());

        mvc.perform(get("/api/applications/" + id).header("Authorization", token))
                .andExpect(status().isNotFound());
    }

    @Test
    void usersAreIsolated() throws Exception {
        String created = mvc.perform(post("/api/applications")
                        .header("Authorization", token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"companyName\":\"ACME\",\"jobTitle\":\"Dev\"}"))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        String id = objectMapper.readTree(created).get("id").asText();

        String bobToken = bearerFor("bob@example.com", "Bob");

        mvc.perform(get("/api/applications/" + id).header("Authorization", bobToken))
                .andExpect(status().isNotFound());
        mvc.perform(get("/api/applications").header("Authorization", bobToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalItems").value(0));
    }

    @Test
    void meReturnsCurrentUser() throws Exception {
        mvc.perform(get("/api/me").header("Authorization", token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email").value("alice@example.com"))
                .andExpect(jsonPath("$.name").value("Alice"));
    }
}
