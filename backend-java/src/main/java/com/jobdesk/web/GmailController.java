package com.jobdesk.web;

import com.jobdesk.domain.User;
import com.jobdesk.repository.UserRepository;
import com.jobdesk.security.GoogleOAuthService;
import com.jobdesk.security.GoogleOAuthService.GoogleTokenResponse;
import com.jobdesk.security.StateSigner;
import com.jobdesk.service.MailScanService;
import com.jobdesk.web.dto.ScanResult;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.net.URI;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

/**
 * Connexion Gmail + scan. Portage du {@code GmailController} Symfony.
 * Endpoints authentifiés sauf {@code /callback} (redirection OAuth du navigateur).
 */
@RestController
@RequestMapping("/api/gmail")
public class GmailController {

    private static final String GMAIL_SCOPE = "https://www.googleapis.com/auth/gmail.readonly";

    private final UserRepository userRepository;
    private final GoogleOAuthService google;
    private final StateSigner stateSigner;
    private final MailScanService mailScanService;
    private final String backendUrl;
    private final String frontendUrl;

    public GmailController(UserRepository userRepository, GoogleOAuthService google,
                           StateSigner stateSigner, MailScanService mailScanService,
                           @Value("${app.backend-url}") String backendUrl,
                           @Value("${app.frontend-url}") String frontendUrl) {
        this.userRepository = userRepository;
        this.google = google;
        this.stateSigner = stateSigner;
        this.mailScanService = mailScanService;
        this.backendUrl = backendUrl;
        this.frontendUrl = frontendUrl;
    }

    @GetMapping("/connect")
    public Map<String, String> connect(@AuthenticationPrincipal User user) {
        String state = stateSigner.sign(user.getId().toString());
        String url = google.authorizationUrl(
                redirectUri(), List.of(GMAIL_SCOPE), state, "offline", "consent");
        return Map.of("url", url);
    }

    @GetMapping("/callback")
    @Transactional
    public ResponseEntity<Void> callback(
            @RequestParam(required = false) String code,
            @RequestParam(required = false) String state,
            @RequestParam(required = false) String error) {
        if (error != null || code == null || state == null) {
            return redirect("/settings?gmail=error");
        }
        String userId = stateSigner.verify(state);
        if (userId == null) {
            return redirect("/settings?gmail=error");
        }
        Optional<User> maybeUser;
        try {
            maybeUser = userRepository.findById(UUID.fromString(userId));
        } catch (IllegalArgumentException e) {
            return redirect("/settings?gmail=error");
        }
        if (maybeUser.isEmpty()) {
            return redirect("/settings?gmail=error");
        }
        try {
            GoogleTokenResponse token = google.exchangeCode(code, redirectUri());
            User user = maybeUser.get();
            user.setGmailToken(token.accessToken());
            if (token.refreshToken() != null) {
                user.setGmailRefreshToken(token.refreshToken());
            }
            userRepository.save(user);
        } catch (Exception e) {
            return redirect("/settings?gmail=error");
        }
        return redirect("/settings?gmail=connected");
    }

    @GetMapping("/status")
    public Map<String, Object> status(@AuthenticationPrincipal User user) {
        var map = new java.util.HashMap<String, Object>();
        map.put("connected", user.getGmailToken() != null);
        map.put("lastMailScanAt", user.getLastMailScanAt() != null
                ? user.getLastMailScanAt().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME) : null);
        return map;
    }

    @DeleteMapping("/disconnect")
    @Transactional
    public ResponseEntity<Void> disconnect(@AuthenticationPrincipal User user) {
        user.setGmailToken(null);
        user.setGmailRefreshToken(null);
        userRepository.save(user);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/scan")
    public ResponseEntity<ScanResult> scan(@AuthenticationPrincipal User user) {
        if (user.getGmailToken() == null) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(ScanResult.error("Gmail non connecté"));
        }
        return ResponseEntity.ok(mailScanService.scanForUser(user));
    }

    private String redirectUri() {
        return backendUrl + "/api/gmail/callback";
    }

    private ResponseEntity<Void> redirect(String frontendPath) {
        return ResponseEntity.status(HttpStatus.FOUND)
                .location(URI.create(frontendUrl + frontendPath)).build();
    }
}
