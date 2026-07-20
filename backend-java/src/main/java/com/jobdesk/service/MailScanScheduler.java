package com.jobdesk.service;

import com.jobdesk.domain.User;
import com.jobdesk.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * Scan Gmail périodique de tous les utilisateurs connectés (toutes les 2 heures).
 * Remplace {@code App\Schedule} + {@code ScanAllMailsHandler} (Symfony Scheduler/Messenger).
 */
@Component
public class MailScanScheduler {

    private static final Logger log = LoggerFactory.getLogger(MailScanScheduler.class);

    private final UserRepository userRepository;
    private final MailScanService mailScanService;

    public MailScanScheduler(UserRepository userRepository, MailScanService mailScanService) {
        this.userRepository = userRepository;
        this.mailScanService = mailScanService;
    }

    @Scheduled(fixedRate = 2, initialDelay = 2, timeUnit = TimeUnit.HOURS)
    public void scanAll() {
        List<User> users = userRepository.findByGmailTokenIsNotNull();
        log.info("Scan Gmail périodique : {} utilisateur(s)", users.size());
        for (User user : users) {
            try {
                mailScanService.scanForUser(user);
            } catch (Exception e) {
                log.warn("Scan échoué pour {}: {}", user.getEmail(), e.getMessage());
            }
        }
    }
}
