package com.jobdesk.web.dto;

/**
 * Champs pré-remplis extraits d'une offre d'emploi. {@code contractType} et {@code source}
 * sont des chaînes correspondant aux valeurs d'enum (ex. "CDI", "linkedin") ; le frontend
 * les injecte tels quels dans le formulaire de création.
 */
public record ScrapeResult(
        String companyName,
        String jobTitle,
        String location,
        String contractType,
        String salaryRange,
        String jobDescription,
        String source
) {
}
