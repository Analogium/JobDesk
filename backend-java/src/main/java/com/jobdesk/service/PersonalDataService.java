package com.jobdesk.service;

import com.jobdesk.domain.Application;
import com.jobdesk.domain.User;
import com.jobdesk.repository.ApplicationRepository;
import com.jobdesk.repository.MailScanRepository;
import com.jobdesk.repository.PasswordResetTokenRepository;
import com.jobdesk.repository.RefreshTokenRepository;
import com.jobdesk.repository.ShareLinkRepository;
import com.jobdesk.repository.UserRepository;
import com.jobdesk.web.dto.ApplicationDto;
import com.jobdesk.web.dto.PersonalDataExport;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Droits des personnes sur leurs données (RGPD) : accès et portabilité (art. 15 et 20),
 * effacement (art. 17).
 */
@Service
public class PersonalDataService {

    private static final Logger log = LoggerFactory.getLogger(PersonalDataService.class);

    private final UserRepository userRepository;
    private final ApplicationRepository applicationRepository;
    private final MailScanRepository mailScanRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final PasswordResetTokenRepository passwordResetTokenRepository;
    private final ShareLinkRepository shareLinkRepository;

    public PersonalDataService(UserRepository userRepository,
                               ApplicationRepository applicationRepository,
                               MailScanRepository mailScanRepository,
                               RefreshTokenRepository refreshTokenRepository,
                               PasswordResetTokenRepository passwordResetTokenRepository,
                               ShareLinkRepository shareLinkRepository) {
        this.userRepository = userRepository;
        this.applicationRepository = applicationRepository;
        this.mailScanRepository = mailScanRepository;
        this.refreshTokenRepository = refreshTokenRepository;
        this.passwordResetTokenRepository = passwordResetTokenRepository;
        this.shareLinkRepository = shareLinkRepository;
    }

    /** Rassemble tout ce que le compte contient, dans un format réutilisable ailleurs. */
    @Transactional(readOnly = true)
    public PersonalDataExport export(User user) {
        List<ApplicationDto> applications = applicationRepository.findByUser(user).stream()
                .map(ApplicationDto::from)
                .toList();

        List<PersonalDataExport.MailScanEntry> scans =
                mailScanRepository.findByUserOrderByScannedAtDesc(user).stream()
                        .map(PersonalDataExport.MailScanEntry::from)
                        .toList();

        PersonalDataExport.ShareLinkEntry shareLink = shareLinkRepository.findByUser(user)
                .map(PersonalDataExport.ShareLinkEntry::from)
                .orElse(null);

        return new PersonalDataExport(
                LocalDateTime.now(),
                PersonalDataExport.Account.from(user),
                applications,
                scans,
                shareLink);
    }

    /**
     * Efface le compte et tout ce qui s'y rattache, sans conserver de copie.
     *
     * <p>La suppression est explicite et ordonnée : les clés étrangères historiques
     * (`application` et `mail_scan` vers `user`) n'ont pas de {@code ON DELETE CASCADE},
     * supprimer l'utilisateur d'abord violerait la contrainte. Les candidatures emportent
     * leur historique et leurs contacts par cascade JPA ; les jetons de session et de
     * réinitialisation par cascade SQL.
     */
    @Transactional
    public void deleteAccount(User user) {
        List<Application> applications = applicationRepository.findByUser(user);
        applicationRepository.deleteAll(applications);
        mailScanRepository.deleteByUser(user);
        passwordResetTokenRepository.deleteByUser(user);
        refreshTokenRepository.deleteByUser(user);
        shareLinkRepository.deleteByUser(user);
        userRepository.delete(user);

        // Journalisé sans l'email : tracer la suppression ne doit pas la contredire.
        log.info("Compte {} supprimé ({} candidatures)", user.getId(), applications.size());
    }
}
