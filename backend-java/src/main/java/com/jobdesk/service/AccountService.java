package com.jobdesk.service;

import com.jobdesk.domain.User;
import com.jobdesk.repository.UserRepository;
import com.jobdesk.web.dto.LoginRequest;
import com.jobdesk.web.dto.RegisterRequest;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.Locale;
import java.util.Optional;

/**
 * Inscription et connexion par email + mot de passe, en complément du login Google
 * porté par {@link com.jobdesk.security.GoogleOAuthService}.
 *
 * <p>Un compte est identifié par son email : selon la façon dont il a été créé il
 * possède un mot de passe, un token Google, ou les deux.
 */
@Service
public class AccountService {

    /**
     * Hash BCrypt d'un mot de passe factice, comparé quand aucun compte ne correspond.
     * Sans ça, une adresse inconnue répondrait bien plus vite qu'une adresse connue,
     * ce qui permettrait d'énumérer les comptes en mesurant le temps de réponse.
     */
    private static final String DUMMY_HASH =
            "$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy";

    /** Message unique : ne jamais révéler si c'est l'email ou le mot de passe qui est faux. */
    private static final String BAD_CREDENTIALS = "Email ou mot de passe incorrect";

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    public AccountService(UserRepository userRepository, PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Transactional
    public User register(RegisterRequest request) {
        String email = normalize(request.email());

        if (userRepository.findByEmailIgnoreCase(email).isPresent()) {
            // Volontairement explicite : un attaquant peut de toute façon découvrir
            // qu'un email est pris en tentant de s'inscrire, et un message vague
            // bloquerait l'utilisateur légitime qui a créé son compte via Google.
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "Un compte existe déjà avec cet email. Connectez-vous, éventuellement avec Google.");
        }

        User user = new User();
        user.setEmail(email);
        user.setName(request.name().trim());
        user.setPasswordHash(passwordEncoder.encode(request.password()));
        return userRepository.save(user);
    }

    @Transactional(readOnly = true)
    public User authenticate(LoginRequest request) {
        Optional<User> found = userRepository.findByEmailIgnoreCase(normalize(request.email()));

        // Le compte peut exister sans mot de passe : il a été créé via Google.
        String hash = found.map(User::getPasswordHash).orElse(null);
        if (hash == null) {
            passwordEncoder.matches(request.password(), DUMMY_HASH);
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, BAD_CREDENTIALS);
        }

        if (!passwordEncoder.matches(request.password(), hash)) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, BAD_CREDENTIALS);
        }

        return found.get();
    }

    private String normalize(String email) {
        return email.trim().toLowerCase(Locale.ROOT);
    }
}
