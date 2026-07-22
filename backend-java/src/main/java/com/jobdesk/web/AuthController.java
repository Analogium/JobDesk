package com.jobdesk.web;

import com.jobdesk.domain.User;
import com.jobdesk.repository.UserRepository;
import com.jobdesk.security.GoogleOAuthService;
import com.jobdesk.security.GoogleOAuthService.GoogleTokenResponse;
import com.jobdesk.security.GoogleOAuthService.GoogleUserInfo;
import com.jobdesk.security.ClientIp;
import com.jobdesk.security.JwtService;
import com.jobdesk.security.RateLimiter;
import com.jobdesk.service.AccountService;
import com.jobdesk.service.PasswordResetService;
import com.jobdesk.service.RefreshTokenService;
import com.jobdesk.web.dto.RefreshRequest;
import com.jobdesk.web.dto.ForgotPasswordRequest;
import com.jobdesk.web.dto.LoginRequest;
import com.jobdesk.web.dto.RegisterRequest;
import com.jobdesk.web.dto.ResetPasswordRequest;
import com.jobdesk.web.dto.TokenResponse;
import com.jobdesk.web.dto.UserDto;
import jakarta.servlet.http.HttpServletRequest;
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
import org.springframework.web.server.ResponseStatusException;

import java.net.URI;
import java.time.Duration;
import java.util.List;
import java.util.Locale;

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
    /** Fenêtres de limitation de débit (voir {@link RateLimiter}). */
    private static final Duration FORGOT_WINDOW = Duration.ofMinutes(15);
    private static final Duration LOGIN_WINDOW = Duration.ofMinutes(5);

    private final AccountService accountService;
    private final PasswordResetService passwordResetService;
    private final RefreshTokenService refreshTokenService;
    private final RateLimiter rateLimiter;
    private final String backendUrl;
    private final String frontendUrl;

    public AuthController(GoogleOAuthService google, UserRepository userRepository, JwtService jwtService,
                          AccountService accountService, PasswordResetService passwordResetService,
                          RefreshTokenService refreshTokenService, RateLimiter rateLimiter,
                          @Value("${app.backend-url}") String backendUrl,
                          @Value("${app.frontend-url}") String frontendUrl) {
        this.rateLimiter = rateLimiter;
        this.google = google;
        this.userRepository = userRepository;
        this.jwtService = jwtService;
        this.accountService = accountService;
        this.passwordResetService = passwordResetService;
        this.refreshTokenService = refreshTokenService;
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
    public ResponseEntity<TokenResponse> login(@Valid @RequestBody LoginRequest request,
                                               HttpServletRequest http) {
        // Freine le bourrinage de mots de passe sans gêner un utilisateur qui se trompe.
        checkRateLimit("login:email:" + request.email().toLowerCase(Locale.ROOT), 10, LOGIN_WINDOW);
        checkRateLimit("login:ip:" + ClientIp.of(http), 30, LOGIN_WINDOW);

        User user = accountService.authenticate(request);
        return ResponseEntity.ok(tokenFor(user));
    }

    /**
     * Répond 204 même si l'adresse est inconnue : une réponse différente permettrait
     * de savoir quelles adresses ont un compte.
     */
    @PostMapping("/password/forgot")
    public ResponseEntity<Void> forgotPassword(@Valid @RequestBody ForgotPasswordRequest request,
                                               HttpServletRequest http) {
        // Appliqué AVANT toute recherche en base, et identiquement que l'adresse existe
        // ou non : sinon un 429 sélectif indiquerait quelles adresses ont un compte.
        // Empêche de vider le quota Brevo en rejouant la demande en boucle.
        checkRateLimit("forgot:email:" + request.email().toLowerCase(Locale.ROOT), 3, FORGOT_WINDOW);
        checkRateLimit("forgot:ip:" + ClientIp.of(http), 10, FORGOT_WINDOW);

        passwordResetService.requestReset(request.email());
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/password/reset")
    public ResponseEntity<Void> resetPassword(@Valid @RequestBody ResetPasswordRequest request) {
        passwordResetService.reset(request.token(), request.password());
        return ResponseEntity.noContent().build();
    }

    private void checkRateLimit(String key, int max, Duration window) {
        if (!rateLimiter.tryAcquire(key, max, window)) {
            throw new ResponseStatusException(HttpStatus.TOO_MANY_REQUESTS,
                    "Trop de tentatives. Réessayez dans quelques minutes.");
        }
    }

    /**
     * Échange un refresh token valide contre un nouvel access token (et un nouveau refresh,
     * l'ancien étant révoqué). C'est ce qui évite de se reconnecter quand le JWT expire.
     */
    @PostMapping("/refresh")
    public ResponseEntity<TokenResponse> refresh(@Valid @RequestBody RefreshRequest request) {
        RefreshTokenService.Rotation rotation = refreshTokenService.rotate(request.refreshToken());
        String jwt = jwtService.generate(rotation.user());
        return ResponseEntity.ok(new TokenResponse(jwt, rotation.refreshToken(), UserDto.from(rotation.user())));
    }

    /** Déconnexion : révoque le refresh token pour que la session ne puisse pas reprendre. */
    @PostMapping("/logout")
    public ResponseEntity<Void> logout(@RequestBody(required = false) RefreshRequest request) {
        if (request != null) {
            refreshTokenService.revoke(request.refreshToken());
        }
        return ResponseEntity.noContent().build();
    }

    /** Émet l'access token (JWT court) + un refresh token, tous deux renvoyés au client. */
    private TokenResponse tokenFor(User user) {
        return new TokenResponse(jwtService.generate(user), refreshTokenService.issue(user), UserDto.from(user));
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

            // Le refresh token voyage dans l'URL au même titre que l'access token : le
            // frontend les récupère sur /auth/callback puis les stocke tous les deux.
            String jwt = jwtService.generate(user);
            String refresh = refreshTokenService.issue(user);
            return redirect(frontendUrl + "/auth/callback?token=" + jwt + "&refresh=" + refresh);
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
