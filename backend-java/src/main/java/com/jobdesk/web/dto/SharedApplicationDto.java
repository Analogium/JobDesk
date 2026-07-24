package com.jobdesk.web.dto;

import com.jobdesk.domain.Application;
import com.jobdesk.domain.ApplicationSource;
import com.jobdesk.domain.ApplicationStatus;
import com.jobdesk.domain.ContractType;
import com.jobdesk.domain.StatusHistory;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

/**
 * Vue <strong>réduite</strong> d'une candidature, exposée via un lien de partage.
 *
 * <p>Volontairement amputée des données personnelles ou de tiers : ni {@code notes} du
 * titulaire, ni {@code contacts} recruteurs. L'historique de statut est conservé mais
 * sans ses notes libres.
 */
public record SharedApplicationDto(
        UUID id,
        String companyName,
        String jobTitle,
        String jobUrl,
        String jobDescription,
        String location,
        ContractType contractType,
        String salaryRange,
        ApplicationStatus status,
        LocalDateTime appliedAt,
        ApplicationSource source,
        LocalDateTime createdAt,
        LocalDateTime updatedAt,
        List<Entry> statusHistories
) {

    /** Historique de statut réduit : le changement et sa date, sans les notes libres. */
    public record Entry(
            ApplicationStatus previousStatus,
            ApplicationStatus newStatus,
            LocalDateTime changedAt,
            String trigger
    ) {
        static Entry from(StatusHistory h) {
            return new Entry(h.getPreviousStatus(), h.getNewStatus(), h.getChangedAt(), h.getTrigger());
        }
    }

    public static SharedApplicationDto from(Application a) {
        return new SharedApplicationDto(
                a.getId(),
                a.getCompanyName(),
                a.getJobTitle(),
                a.getJobUrl(),
                a.getJobDescription(),
                a.getLocation(),
                a.getContractType(),
                a.getSalaryRange(),
                a.getStatus(),
                a.getAppliedAt(),
                a.getSource(),
                a.getCreatedAt(),
                a.getUpdatedAt(),
                a.getStatusHistories().stream().map(Entry::from).toList()
        );
    }
}
