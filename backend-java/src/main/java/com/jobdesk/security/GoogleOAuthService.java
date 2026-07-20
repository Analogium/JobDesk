package com.jobdesk.security;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestClient;
import org.springframework.web.util.UriComponentsBuilder;

import java.util.List;

/**
 * Flow OAuth2 Google (code d'autorisation) réalisé « à la main » via {@link RestClient},
 * remplaçant knpu/oauth2-client + league/oauth2-google de l'ancien backend.
 */
@Service
public class GoogleOAuthService {

    private static final String AUTH_URL = "https://accounts.google.com/o/oauth2/v2/auth";
    private static final String TOKEN_URL = "https://oauth2.googleapis.com/token";
    private static final String USERINFO_URL = "https://www.googleapis.com/oauth2/v3/userinfo";

    private final String clientId;
    private final String clientSecret;
    private final RestClient http = RestClient.create();

    public GoogleOAuthService(
            @Value("${app.google.client-id}") String clientId,
            @Value("${app.google.client-secret}") String clientSecret) {
        this.clientId = clientId;
        this.clientSecret = clientSecret;
    }

    /** Construit l'URL de consentement Google. */
    public String authorizationUrl(String redirectUri, List<String> scopes, String state,
                                   String accessType, String prompt) {
        UriComponentsBuilder b = UriComponentsBuilder.fromUriString(AUTH_URL)
                .queryParam("client_id", clientId)
                .queryParam("redirect_uri", redirectUri)
                .queryParam("response_type", "code")
                .queryParam("scope", String.join(" ", scopes))
                .queryParam("state", state);
        if (accessType != null) {
            b.queryParam("access_type", accessType);
        }
        if (prompt != null) {
            b.queryParam("prompt", prompt);
        }
        // encode() : les scopes séparés par des espaces doivent être percent-encodés.
        return b.encode().build().toUriString();
    }

    /** Échange le code d'autorisation contre des tokens. */
    public GoogleTokenResponse exchangeCode(String code, String redirectUri) {
        MultiValueMap<String, String> form = new LinkedMultiValueMap<>();
        form.add("code", code);
        form.add("client_id", clientId);
        form.add("client_secret", clientSecret);
        form.add("redirect_uri", redirectUri);
        form.add("grant_type", "authorization_code");

        return http.post()
                .uri(TOKEN_URL)
                .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                .body(form)
                .retrieve()
                .body(GoogleTokenResponse.class);
    }

    /** Rafraîchit un access token à partir d'un refresh token (grant refresh_token). */
    public GoogleTokenResponse refreshAccessToken(String refreshToken) {
        MultiValueMap<String, String> form = new LinkedMultiValueMap<>();
        form.add("client_id", clientId);
        form.add("client_secret", clientSecret);
        form.add("refresh_token", refreshToken);
        form.add("grant_type", "refresh_token");

        return http.post()
                .uri(TOKEN_URL)
                .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                .body(form)
                .retrieve()
                .body(GoogleTokenResponse.class);
    }

    /** Récupère le profil OpenID (email, nom, avatar) à partir d'un access token. */
    public GoogleUserInfo fetchUserInfo(String accessToken) {
        return http.get()
                .uri(USERINFO_URL)
                .header("Authorization", "Bearer " + accessToken)
                .retrieve()
                .body(GoogleUserInfo.class);
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record GoogleTokenResponse(
            @JsonProperty("access_token") String accessToken,
            @JsonProperty("refresh_token") String refreshToken,
            @JsonProperty("expires_in") Long expiresIn,
            @JsonProperty("scope") String scope,
            @JsonProperty("token_type") String tokenType) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record GoogleUserInfo(
            String sub,
            String email,
            String name,
            String picture) {
    }
}
