package com.jobdesk.service;

import com.jobdesk.domain.MailScan;
import com.jobdesk.domain.PasswordResetToken;
import com.jobdesk.domain.RefreshToken;
import com.jobdesk.domain.User;
import com.jobdesk.repository.ApplicationRepository;
import com.jobdesk.repository.MailScanRepository;
import com.jobdesk.repository.PasswordResetTokenRepository;
import com.jobdesk.repository.RefreshTokenRepository;
import com.jobdesk.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Purge de conservation : elle doit retirer le périmé et rien d'autre.
 */
@SpringBootTest
@ActiveProfiles("test")
class DataRetentionSchedulerTest {

    @Autowired
    DataRetentionScheduler scheduler;
    @Autowired
    UserRepository userRepository;
    @Autowired
    ApplicationRepository applicationRepository;
    @Autowired
    MailScanRepository mailScanRepository;
    @Autowired
    RefreshTokenRepository refreshTokenRepository;
    @Autowired
    PasswordResetTokenRepository passwordResetTokenRepository;

    private User user;

    @BeforeEach
    void setUp() {
        refreshTokenRepository.deleteAll();
        passwordResetTokenRepository.deleteAll();
        mailScanRepository.deleteAll();
        applicationRepository.deleteAll();
        userRepository.deleteAll();

        user = new User();
        user.setEmail("alice@example.com");
        user.setName("Alice");
        user = userRepository.save(user);
    }

    private RefreshToken refreshToken(LocalDateTime expiresAt, LocalDateTime revokedAt) {
        RefreshToken t = new RefreshToken();
        t.setUser(user);
        t.setTokenHash("hash-" + java.util.UUID.randomUUID());
        t.setExpiresAt(expiresAt);
        t.setRevokedAt(revokedAt);
        return refreshTokenRepository.save(t);
    }

    private MailScan mailScan(LocalDateTime scannedAt) {
        MailScan s = new MailScan();
        s.setUser(user);
        s.setScannedAt(scannedAt);
        return mailScanRepository.save(s);
    }

    @Test
    void removesExpiredRefreshTokensButKeepsValidOnes() {
        refreshToken(LocalDateTime.now().minusDays(1), null);   // expiré
        RefreshToken valid = refreshToken(LocalDateTime.now().plusDays(10), null);

        scheduler.purgeExpiredData();

        assertThat(refreshTokenRepository.findAll()).extracting(RefreshToken::getId)
                .containsExactly(valid.getId());
    }

    /**
     * Un jeton récemment révoqué est conservé un temps : c'est sa présence qui permet
     * de détecter un rejeu. Passé le délai de garde, il est purgé.
     */
    @Test
    void keepsRecentlyRevokedTokensAndDropsOldOnes() {
        RefreshToken recent = refreshToken(LocalDateTime.now().plusDays(10), LocalDateTime.now().minusDays(1));
        refreshToken(LocalDateTime.now().plusDays(10), LocalDateTime.now().minusDays(60));

        scheduler.purgeExpiredData();

        assertThat(refreshTokenRepository.findAll()).extracting(RefreshToken::getId)
                .containsExactly(recent.getId());
    }

    @Test
    void removesUsedAndExpiredPasswordResetLinks() {
        PasswordResetToken used = new PasswordResetToken();
        used.setUser(user);
        used.setTokenHash("hash-used");
        used.setExpiresAt(LocalDateTime.now().plusHours(1));
        used.setUsedAt(LocalDateTime.now());
        passwordResetTokenRepository.save(used);

        PasswordResetToken expired = new PasswordResetToken();
        expired.setUser(user);
        expired.setTokenHash("hash-expired");
        expired.setExpiresAt(LocalDateTime.now().minusHours(1));
        passwordResetTokenRepository.save(expired);

        PasswordResetToken pending = new PasswordResetToken();
        pending.setUser(user);
        pending.setTokenHash("hash-pending");
        pending.setExpiresAt(LocalDateTime.now().plusHours(1));
        passwordResetTokenRepository.save(pending);

        scheduler.purgeExpiredData();

        assertThat(passwordResetTokenRepository.findAll())
                .extracting(PasswordResetToken::getTokenHash)
                .containsExactly("hash-pending");
    }

    @Test
    void removesMailScanLogsOlderThanTheRetentionWindow() {
        mailScan(LocalDateTime.now().minusDays(400));
        MailScan recent = mailScan(LocalDateTime.now().minusDays(10));

        scheduler.purgeExpiredData();

        assertThat(mailScanRepository.findAll()).extracting(MailScan::getId)
                .containsExactly(recent.getId());
    }

    /** La purge est technique : elle ne doit jamais toucher aux comptes ni aux candidatures. */
    @Test
    void neverTouchesAccountsOrApplications() {
        com.jobdesk.domain.Application app = new com.jobdesk.domain.Application();
        app.setUser(user);
        app.setCompanyName("Acme");
        app.setJobTitle("Développeur");
        applicationRepository.save(app);

        refreshToken(LocalDateTime.now().minusDays(1), null);

        scheduler.purgeExpiredData();

        assertThat(userRepository.count()).isEqualTo(1);
        assertThat(applicationRepository.count()).isEqualTo(1);
    }
}
