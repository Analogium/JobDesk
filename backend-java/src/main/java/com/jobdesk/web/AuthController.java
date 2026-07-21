package com.jobdesk.web;

import com.jobdesk.domain.User;
import com.jobdesk.repository.UserRepository;
import com.jobdesk.security.GoogleOAuthService;
import com.jobdesk.security.GoogleOAuthService.GoogleTokenResponse;
import com.jobdesk.security.GoogleOAuthService.GoogleUserInfo;
import com.jobdesk.security.JwtService;
import com.jobdesk.service.AccountService;
import com.jobdesk.service.PasswordResetService;
import com.jobdesk.web.dto.ForgotPasswordRequest;
import com.jobdesk.web.dto.LoginRequest;
import com.jobdesk.web.dto.RegisterRequest;
import com.jobdesk.web.dto.ResetPasswordRequest;
import com.jobdesk.web.dto.TokenResponse;
import com.jobdesk.web.dto.UserDto;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.net.URI;
import java.util.List;

/**
 * Login Google → JWT, remplaçant {@code SecurityController} + {@code GoogleAuthenticator}.
 * Le frontend ouvre {@code /auth/google} puis récupère le token sur
 * {@code FRONTEND_URL/auth/callback?token=...}.
 */
@RestController
@RequestMapping("/auth")
public class AuthController {

    private final GoogleOAuthService google;
    private final UserRepository userRepository;
    private final JwtService jwtService;
    private final AccountService accountService;
    private final PasswordResetService passwordResetService;
    private final String backendUrl;
    private final String frontendUrl;

    public AuthController(GoogleOAuthService google, UserRepository userRepository, JwtService jwtService,
                          AccountService accountService, PasswordResetService passwordResetService,
                          @Value("${app.backend-url}") String backendUrl,
                          @Value("${app.frontend-url}") String frontendUrl) {
        this.google = google;
        this.userRepository = userRepository;
        this.jwtService = jwtService;
        this.accountService = accountService;
        this.passwordResetService = passwordResetService;
        this.backendUrl = backendUrl;
        this.frontendUrl = frontendUrl;
    }

    /** Inscription email + mot de passe. Renvoie directement un JWT : pas d'étape de vérification. */
    @PostMapping("/register")
    public ResponseEntity<TokenResponse> register(@Valid @RequestBody RegisterRequest request) {
        User user = accountService.register(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(tokenFor(user));
    }

    @PostMapping("/login")
    public ResponseEntity<TokenResponse> login(@Valid @RequestBody LoginRequest request) {
        User user = accountService.authenticate(request);
        return ResponseEntity.ok(tokenFor(user));
    }

    /**
     * Répond 204 même si l'adresse est inconnue : une réponse différente permettrait
     * de savoir quelles adresses ont un compte.
     */
    @PostMapping("/password/forgot")
    public ResponseEntity<Void> forgotPassword(@Valid @RequestBody ForgotPasswordRequest request) {
        passwordResetService.requestReset(request.email());
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/password/reset")
    public ResponseEntity<Void> resetPassword(@Valid @RequestBody ResetPasswordRequest request) {
        passwordResetService.reset(request.token(), request.password());
        return ResponseEntity.noContent().build();
    }

    private TokenResponse tokenFor(User user) {
        return new TokenResponse(jwtService.generate(user), UserDto.from(user));
    }

    @GetMapping("/google")
    public ResponseEntity<Void> connectGoogle() {
        String url = google.authorizationUrl(
                redirectUri(),
                List.of("openid", "email", "profile"),
                "login",
                null,
                null);
        return ResponseEntity.status(HttpStatus.FOUND).location(URI.create(url)).build();
    }

    @GetMapping("/google/check")
    @Transactional
    public ResponseEntity<Void> connectGoogleCheck(
            @RequestParam(required = false) String code,
            @RequestParam(required = false) String error) {
        if (error != null || code == null) {
            return redirect(frontendUrl + "/auth/login?error=google");
        }
        try {
            GoogleTokenResponse token = google.exchangeCode(code, redirectUri());
            GoogleUserInfo info = google.fetchUserInfo(token.accessToken());

            User user = userRepository.findByEmail(info.email()).orElseGet(User::new);
            user.setEmail(info.email());
            user.setName(info.name() != null ? info.name() : info.email());
            user.setAvatarUrl(info.picture());
            user.setGoogleToken(token.accessToken());
            user = userRepository.save(user);

            String jwt = jwtService.generate(user);
            return redirect(frontendUrl + "/auth/callback?token=" + jwt);
        } catch (Exception e) {
            return redirect(frontendUrl + "/auth/login?error=google");
        }
    }

    private String redirectUri() {
        return backendUrl + "/auth/google/check";
    }

    private ResponseEntity<Void> redirect(String url) {
        return ResponseEntity.status(HttpStatus.FOUND).location(URI.create(url)).build();
    }
}
