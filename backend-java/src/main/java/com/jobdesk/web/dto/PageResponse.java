package com.jobdesk.web.dto;

import java.util.List;

/**
 * Enveloppe de collection conservant le contrat attendu par le frontend Nuxt
 * ({@code data.member} / {@code data.totalItems}), hérité d'API Platform.
 */
public record PageResponse<T>(
        List<T> member,
        long totalItems
) {
}
