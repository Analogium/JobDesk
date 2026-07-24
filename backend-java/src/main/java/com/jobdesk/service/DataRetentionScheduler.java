package com.jobdesk.service;

import com.jobdesk.repository.MailScanRepository;
import com.jobdesk.repository.PasswordResetTokenRepository;
import com.jobdesk.repository.RefreshTokenRepository;
import com.jobdesk.repository.ShareLinkRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.concurrent.TimeUnit;

/**
 * Limitation de la durée de conservation (RGPD art. 5.1.e) : purge quotidienne des
 * données devenues inutiles.
 *
 * <p>Ne touche <strong>jamais</strong> aux comptes ni aux candidatures : seules les
 * données techniques périmées sont supprimées. L'effacement d'un compte reste une
 * décision de l'utilisateur ({@link PersonalDataService#deleteAccount}).
 */
@Component
public class DataRetentionScheduler {

    private static final Logger log = LoggerFactory.getLogger(DataRetentionScheduler.class);

    /**
     * Délai de garde des jetons révoqués : au-delà, ils ne servent plus à repérer un
     * rejeu et n'ont donc plus de raison d'être conservés.
     */
    private static final int REVOKED_TOKEN_GRACE_DAYS = 30;

    private final RefreshTokenRepository refreshTokenRepository;
    private final PasswordResetTokenRepository passwordResetTokenRepository;
    private final MailScanRepository mailScanRepository;
    private final ShareLinkRepository shareLinkRepository;
    private final int mailScanRetentionDays;

    public DataRetentionScheduler(RefreshTokenRepository refreshTokenRepository,
                                  PasswordResetTokenRepository passwordResetTokenRepository,
                                  MailScanRepository mailScanRepository,
                                  ShareLinkRepository shareLinkRepository,
                                  @Value("${app.retention.mail-scan-days:365}") int mailScanRetentionDays) {
        this.refreshTokenRepository = refreshTokenRepository;
        this.passwordResetTokenRepository = passwordResetTokenRepository;
        this.mailScanRepository = mailScanRepository;
        this.shareLinkRepository = shareLinkRepository;
        this.mailScanRetentionDays = mailScanRetentionDays;
    }

    @Scheduled(fixedRate = 24, initialDelay = 1, timeUnit = TimeUnit.HOURS)
    @Transactional
    public void purgeExpiredData() {
        LocalDateTime now = LocalDateTime.now();

        int refreshTokens = refreshTokenRepository.deleteUnusable(
                now, now.minusDays(REVOKED_TOKEN_GRACE_DAYS));
        int resetTokens = passwordResetTokenRepository.deleteUnusable(now);
        int scans = mailScanRepository.deleteScannedBefore(now.minusDays(mailScanRetentionDays));
        int shareLinks = shareLinkRepository.deleteExpired(now);

        if (refreshTokens + resetTokens + scans + shareLinks > 0) {
            log.info("Purge RGPD : {} refresh token(s), {} lien(s) de réinitialisation, "
                            + "{} journal(aux) de scan, {} lien(s) de partage expiré(s)",
                    refreshTokens, resetTokens, scans, shareLinks);
        }
    }
}
