package com.jobdesk.domain;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

/**
 * Source d'une candidature. ⚠️ Stocké EN MINUSCULE en base (ex. "linkedin", "manual"),
 * alors que les constantes Java sont en majuscule → mapping via {@code value}.
 * Persistance : {@link com.jobdesk.domain.converter.ApplicationSourceConverter}.
 * Sérialisation JSON : minuscule (attendu par le frontend).
 */
public enum ApplicationSource {
    LINKEDIN("linkedin"),
    WTTJ("wttj"),
    INDEED("indeed"),
    MANUAL("manual"),
    OTHER("other");

    private final String value;

    ApplicationSource(String value) {
        this.value = value;
    }

    @JsonValue
    public String getValue() {
        return value;
    }

    @JsonCreator
    public static ApplicationSource fromValue(String raw) {
        if (raw == null) {
            return null;
        }
        for (ApplicationSource s : values()) {
            if (s.value.equalsIgnoreCase(raw) || s.name().equalsIgnoreCase(raw)) {
                return s;
            }
        }
        throw new IllegalArgumentException("Source inconnue: " + raw);
    }
}
