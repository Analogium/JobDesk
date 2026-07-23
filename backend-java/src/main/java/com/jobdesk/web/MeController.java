package com.jobdesk.web;

import com.jobdesk.domain.User;
import com.jobdesk.service.PersonalDataService;
import com.jobdesk.web.dto.DeleteAccountRequest;
import com.jobdesk.web.dto.PersonalDataExport;
import com.jobdesk.web.dto.UserDto;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDate;

/** Compte de l'utilisateur courant et exercice de ses droits RGPD. */
@RestController
public class MeController {

    private final PersonalDataService personalDataService;
    private final PasswordEncoder passwordEncoder;

    public MeController(PersonalDataService personalDataService, PasswordEncoder passwordEncoder) {
        this.personalDataService = personalDataService;
        this.passwordEncoder = passwordEncoder;
    }

    @GetMapping("/api/me")
    public UserDto me(@AuthenticationPrincipal User user) {
        return UserDto.from(user);
    }

    /**
     * Droit d'accès et portabilité (RGPD art. 15 et 20) : renvoie l'intégralité des
     * données du compte en JSON, servi en téléchargement.
     */
    @GetMapping("/api/me/export")
    public ResponseEntity<PersonalDataExport> export(@AuthenticationPrincipal User user) {
        String filename = "jobdesk-donnees-" + LocalDate.now() + ".json";
        return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_JSON)
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + filename + "\"")
                .body(personalDataService.export(user));
    }

    /**
     * Droit à l'effacement (RGPD art. 17). Irréversible et immédiat : aucune corbeille,
     * aucune conservation différée.
     */
    @DeleteMapping("/api/me")
    public ResponseEntity<Void> deleteAccount(@AuthenticationPrincipal User user,
                                              @RequestBody(required = false) DeleteAccountRequest request) {
        // Un compte protégé par mot de passe le redemande : le JWT seul ne suffit pas
        // pour une action irréversible (session laissée ouverte sur un poste partagé).
        if (user.getPasswordHash() != null) {
            String password = request != null ? request.password() : null;
            if (password == null || !passwordEncoder.matches(password, user.getPasswordHash())) {
                throw new ResponseStatusException(HttpStatus.FORBIDDEN,
                        "Mot de passe incorrect.");
            }
        }

        personalDataService.deleteAccount(user);
        return ResponseEntity.noContent().build();
    }
}
