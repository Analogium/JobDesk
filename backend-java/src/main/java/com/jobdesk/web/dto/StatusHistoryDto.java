package com.jobdesk.web.dto;

import com.jobdesk.domain.ApplicationStatus;
import com.jobdesk.domain.StatusHistory;

import java.time.LocalDateTime;
import java.util.UUID;

public record StatusHistoryDto(
        UUID id,
        ApplicationStatus previousStatus,
        ApplicationStatus newStatus,
        LocalDateTime changedAt,
        String trigger,
        String notes
) {
    public static StatusHistoryDto from(StatusHistory h) {
        return new StatusHistoryDto(
                h.getId(),
                h.getPreviousStatus(),
                h.getNewStatus(),
                h.getChangedAt(),
                h.getTrigger(),
                h.getNotes()
        );
    }
}
