package com.jobdesk.web.dto;

import com.jobdesk.domain.Contact;

import java.util.UUID;

public record ContactDto(
        UUID id,
        String name,
        String email,
        String role,
        String notes
) {
    public static ContactDto from(Contact c) {
        return new ContactDto(
                c.getId(),
                c.getName(),
                c.getEmail(),
                c.getRole(),
                c.getNotes()
        );
    }
}
