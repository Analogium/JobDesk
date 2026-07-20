package com.jobdesk.domain;

/**
 * Type de contrat. Les constantes == valeurs stockées en base,
 * donc mappé via @Enumerated(EnumType.STRING).
 */
public enum ContractType {
    CDI,
    CDD,
    FREELANCE,
    INTERNSHIP,
    ALTERNANCE,
    OTHER
}
