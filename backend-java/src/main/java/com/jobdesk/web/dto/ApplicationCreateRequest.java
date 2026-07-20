package com.jobdesk.web.dto;

import com.jobdesk.domain.ApplicationSource;
import com.jobdesk.domain.ApplicationStatus;
import com.jobdesk.domain.ContractType;
import jakarta.validation.constraints.NotBlank;

/**
 * Charge de création d'une candidature (équivalent {@code application:write} en POST).
 * {@code appliedAt} est reçu en chaîne (date ou date-heure ISO) et parsé côté service.
 */
public record ApplicationCreateRequest(
        @NotBlank String companyName,
        @NotBlank String jobTitle,
        String jobUrl,
        String jobDescription,
        String location,
        ContractType contractType,
        String salaryRange,
        ApplicationStatus status,
        String appliedAt,
        ApplicationSource source,
        String notes
) {
}
