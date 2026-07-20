package com.jobdesk.web;

import com.jobdesk.domain.User;
import com.jobdesk.repository.UserRepository;
import com.jobdesk.security.JwtService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.ActiveProfiles;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Non-régression : le frontend envoie des paramètres avec crochets NON encodés
 * (ex. {@code order[createdAt]=desc}). Tomcat les rejette (400) sauf configuration
 * {@code server.tomcat.relaxed-query-chars}. Ce test tourne sur un vrai Tomcat
 * (RANDOM_PORT) car MockMvc ne reproduit pas la validation d'URI.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
class RawQueryBracketsTest {

    @LocalServerPort
    int port;
    @Autowired
    TestRestTemplate rest;
    @Autowired
    UserRepository userRepository;
    @Autowired
    JwtService jwtService;

    private String token;

    @BeforeEach
    void setUp() {
        userRepository.deleteAll();
        User u = new User();
        u.setEmail("alice@example.com");
        u.setName("Alice");
        token = jwtService.generate(userRepository.save(u));
    }

    @Test
    void acceptsUnencodedBracketsInQuery() {
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(token);
        String url = "http://localhost:" + port + "/api/applications?order[createdAt]=desc&page=1";

        ResponseEntity<String> res = rest.exchange(
                url, HttpMethod.GET, new HttpEntity<>(headers), String.class);

        assertThat(res.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(res.getBody()).contains("\"totalItems\"");
    }
}
