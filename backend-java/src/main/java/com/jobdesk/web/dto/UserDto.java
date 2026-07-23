package com.jobdesk.web.dto;

import com.jobdesk.domain.User;

import java.time.LocalDateTime;
import java.util.UUID;

public record UserDto(
        UUID id,
        String email,
        String name,
        String avatarUrl,
        /** Présence d'un mot de passe, jamais sa valeur : l'UI en a besoin pour savoir
         *  s'il faut le redemander (un compte Google n'en a pas). */
        boolean hasPassword,
        LocalDateTime lastMailScanAt,
        LocalDateTime createdAt
) {
    public static UserDto from(User u) {
        return new UserDto(
                u.getId(),
                u.getEmail(),
                u.getName(),
                u.getAvatarUrl(),
                u.getPasswordHash() != null,
                u.getLastMailScanAt(),
                u.getCreatedAt()
        );
    }
}
