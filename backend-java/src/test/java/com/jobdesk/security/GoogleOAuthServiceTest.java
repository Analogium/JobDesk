package com.jobdesk.security;

import org.junit.jupiter.api.Test;

import java.net.URI;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;

class GoogleOAuthServiceTest {

    private final GoogleOAuthService service = new GoogleOAuthService("client-id", "client-secret");

    @Test
    void authorizationUrlEncodesScopesAndIsValidUri() {
        String url = service.authorizationUrl(
                "http://localhost:8001/auth/google/check",
                List.of("openid", "email", "profile"),
                "login", "offline", "consent");

        // Les espaces entre scopes doivent être percent-encodés (%20), pas bruts.
        assertThat(url).contains("scope=openid%20email%20profile");
        assertThat(url).doesNotContain("scope=openid email profile");
        // Doit être parsable en URI (c'est ce que fait AuthController via URI.create()).
        assertThatCode(() -> URI.create(url)).doesNotThrowAnyException();
    }
}
