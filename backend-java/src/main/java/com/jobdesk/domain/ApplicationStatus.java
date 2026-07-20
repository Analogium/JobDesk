package com.jobdesk.domain;

/**
 * Statut d'une candidature. Les constantes == valeurs stockées en base
 * (colonne varchar), donc mappé directement via @Enumerated(EnumType.STRING).
 */
public enum ApplicationStatus {
    DRAFT,
    APPLIED,
    WAITING,
    RELAUNCH,
    INTERVIEW,
    OFFER,
    REFUSED,
    ABANDONED
}
