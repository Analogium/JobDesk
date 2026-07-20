package com.jobdesk.web.dto;

import com.fasterxml.jackson.annotation.JsonInclude;

/** Résultat d'un scan Gmail (contrat frontend : mailsAnalyzed / matchesFound). */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record ScanResult(
        int mailsAnalyzed,
        int matchesFound,
        String error
) {
    public static ScanResult ok(int analyzed, int matches) {
        return new ScanResult(analyzed, matches, null);
    }

    public static ScanResult error(String message) {
        return new ScanResult(0, 0, message);
    }
}
