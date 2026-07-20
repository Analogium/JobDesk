package com.jobdesk.web.dto;

import com.jobdesk.domain.User;

import java.time.LocalDateTime;
import java.util.UUID;

public record UserDto(
        UUID id,
        String email,
        String name,
        String avatarUrl,
        LocalDateTime lastMailScanAt,
        LocalDateTime createdAt
) {
    public static UserDto from(User u) {
        return new UserDto(
                u.getId(),
                u.getEmail(),
                u.getName(),
                u.getAvatarUrl(),
                u.getLastMailScanAt(),
                u.getCreatedAt()
        );
    }
}
