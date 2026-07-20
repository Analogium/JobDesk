package com.jobdesk.domain.converter;

import com.jobdesk.domain.ApplicationSource;
import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

/**
 * Mappe {@link ApplicationSource} sur sa valeur minuscule stockée en base
 * (colonne {@code source} : "linkedin", "wttj", "indeed", "manual", "other").
 */
@Converter(autoApply = false)
public class ApplicationSourceConverter implements AttributeConverter<ApplicationSource, String> {

    @Override
    public String convertToDatabaseColumn(ApplicationSource attribute) {
        return attribute == null ? null : attribute.getValue();
    }

    @Override
    public ApplicationSource convertToEntityAttribute(String dbData) {
        return ApplicationSource.fromValue(dbData);
    }
}
