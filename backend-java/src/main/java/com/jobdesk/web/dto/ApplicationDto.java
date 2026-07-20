package com.jobdesk.web.dto;

import com.jobdesk.domain.Application;
import com.jobdesk.domain.ApplicationSource;
import com.jobdesk.domain.ApplicationStatus;
import com.jobdesk.domain.ContractType;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

/**
 * Vue lecture d'une candidature (équivalent du groupe de sérialisation
 * {@code application:read}). N'expose jamais l'utilisateur ni les tokens.
 */
public record ApplicationDto(
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
        String notes,
        LocalDateTime createdAt,
        LocalDateTime updatedAt,
        List<StatusHistoryDto> statusHistories,
        List<ContactDto> contacts
) {
    public static ApplicationDto from(Application a) {
        return new ApplicationDto(
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
                a.getNotes(),
                a.getCreatedAt(),
                a.getUpdatedAt(),
                a.getStatusHistories().stream().map(StatusHistoryDto::from).toList(),
                a.getContacts().stream().map(ContactDto::from).toList()
        );
    }
}
