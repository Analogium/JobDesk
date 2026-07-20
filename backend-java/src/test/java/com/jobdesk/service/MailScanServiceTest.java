package com.jobdesk.service;

import com.jobdesk.domain.ApplicationStatus;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class MailScanServiceTest {

    @Test
    void detectsRefused() {
        assertThat(MailScanService.detectStatus("Nous avons le regret de vous informer"))
                .isEqualTo(ApplicationStatus.REFUSED);
        assertThat(MailScanService.detectStatus("Unfortunately, we won't be moving forward"))
                .isEqualTo(ApplicationStatus.REFUSED);
    }

    @Test
    void detectsInterview() {
        assertThat(MailScanService.detectStatus("Invitation à un entretien la semaine prochaine"))
                .isEqualTo(ApplicationStatus.INTERVIEW);
        assertThat(MailScanService.detectStatus("Let's schedule a call to discuss next steps"))
                .isEqualTo(ApplicationStatus.INTERVIEW);
    }

    @Test
    void detectsOffer() {
        assertThat(MailScanService.detectStatus("We are pleased to offer you the position"))
                .isEqualTo(ApplicationStatus.OFFER);
        assertThat(MailScanService.detectStatus("Proposition d'embauche - bienvenue dans l'équipe"))
                .isEqualTo(ApplicationStatus.OFFER);
    }

    @Test
    void refusedTakesPrecedenceOverInterview() {
        // "entretien" (interview) + "regret" (refused) → refused l'emporte (ordre de détection)
        assertThat(MailScanService.detectStatus("Suite à votre entretien, nous avons le regret"))
                .isEqualTo(ApplicationStatus.REFUSED);
    }

    @Test
    void noKeywordReturnsNull() {
        assertThat(MailScanService.detectStatus("Votre commande a été expédiée")).isNull();
    }

    @Test
    void normalizeStripsAccentsAndLegalSuffixes() {
        assertThat(MailScanService.normalizeCompanyName("Décathlon SAS")).isEqualTo("decathlon");
        assertThat(MailScanService.normalizeCompanyName("Bergé & Associés SARL")).isEqualTo("berge associes");
        assertThat(MailScanService.normalizeCompanyName("ACME Corp")).isEqualTo("acme");
    }
}
