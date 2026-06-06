package com.example.scienceops.volunteer;

import java.time.LocalDateTime;

public record VolunteerApplicationResponse(
        String id,
        String activityId,
        String activityTitle,
        String positionId,
        String positionName,
        String name,
        String phone,
        String unitName,
        String ageGroup,
        String availableTimeNote,
        String experienceNote,
        String remark,
        String status,
        String reviewedBy,
        LocalDateTime reviewedAt,
        String reviewNote,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
}
