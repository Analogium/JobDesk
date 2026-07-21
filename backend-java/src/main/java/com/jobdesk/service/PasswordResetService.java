package com.jobdesk.service;

import com.jobdesk.domain.PasswordResetToken;
import com.jobdesk.domain.User;
import com.jobdesk.repository.PasswordResetTokenRepository;
import com.jobdesk.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.Base64;
import java.util.HexFormat;
import java.util.Locale;
import java.util.Optional;

/**
 * Mot de passe oublié : demande d'un lien, puis définition d'un nouveau mot de passe.
 *
 * <p>Un compte créé via Google peut aussi passer par ici : recevoir le mail prouve qu'on
 * possède l'adresse, ce qui suffit à lui ajouter un mot de passe.
 */
@Service
public class PasswordResetService {

    private static final Logger log = LoggerFactory.getLogger(PasswordResetService.class);

    /** 32 octets tirés d'un SecureRandom : impossible à deviner par force brute. */
    private static final int TOKEN_BYTES = 32;

    private final UserRepository userRepository;
    private final PasswordResetTokenRepository tokenRepository;
    private final PasswordEncoder passwordEncoder;
    private final MailService mailService;
    private final SecureRandom random = new SecureRandom();
    private final String frontendUrl;
    private final long ttlSeconds;

    public PasswordResetService(UserRepository userRepository,
                                PasswordResetTokenRepository tokenRepository,
                                PasswordEncoder passwordEncoder,
                                MailService mailService,
                                @Value("${app.frontend-url}") String frontendUrl,
                                @Value("${app.password-reset.ttl-seconds:3600}") long ttlSeconds) {
        this.userRepository = userRepository;
        this.tokenRepository = tokenRepository;
        this.passwordEncoder = passwordEncoder;
        this.mailService = mailService;
        this.frontendUrl = frontendUrl;
        this.ttlSeconds = ttlSeconds;
    }

    /**
     * Toujours silencieux : que l'adresse existe ou non, l'appelant reçoit la même
     * réponse, sinon l'endpoint permettrait de savoir qui a un compte.
     */
    @Transactional
    public void requestReset(String email) {
        Optional<User> found = userRepository.findByEmailIgnoreCase(normalize(email));
        if (found.isEmpty()) {
            log.info("Demande de réinitialisation pour une adresse inconnue, ignorée");
            return;
        }

        User user = found.get();
        // Une demande invalide les liens précédents : un seul lien actif à la fois.
        tokenRepository.deleteByUser(user);

        byte[] raw = new byte[TOKEN_BYTES];
        random.nextBytes(raw);
        String token = Base64.getUrlEncoder().withoutPadding().encodeToString(raw);

        PasswordResetToken entity = new PasswordResetToken();
        entity.setUser(user);
        entity.setTokenHash(sha256(token));
        entity.setExpiresAt(LocalDateTime.now().plusSeconds(ttlSeconds));
        tokenRepository.save(entity);

        mailService.sendPasswordReset(user.getEmail(), frontendUrl + "/auth/reset?token=" + token);
    }

    @Transactional
    public void reset(String token, String newPassword) {
        PasswordResetToken entity = tokenRepository.findByTokenHash(sha256(token))
                .filter(t -> t.isUsable(LocalDateTime.now()))
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST,
                        "Ce lien de réinitialisation est invalide ou expiré. Refaites une demande."));

        User user = entity.getUser();
        user.setPasswordHash(passwordEncoder.encode(newPassword));
        userRepository.save(user);

        // Usage unique : le lien ne doit pas pouvoir resservir s'il traîne dans une boîte mail.
        entity.setUsedAt(LocalDateTime.now());
        tokenRepository.save(entity);
    }

    private String sha256(String value) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            return HexFormat.of().formatHex(digest.digest(value.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 indisponible", e);
        }
    }

    private String normalize(String email) {
        return email.trim().toLowerCase(Locale.ROOT);
    }
}
