package com.example.scienceops.volunteer;

import java.time.LocalDateTime;

record VolunteerApplicationRecord(
        Long id,
        Long activityId,
        String activityTitle,
        Long positionId,
        String positionName,
        String name,
        String phone,
        String unitName,
        String ageGroup,
        String availableTimeNote,
        String experienceNote,
        String remark,
        String status,
        Long reviewedBy,
        LocalDateTime reviewedAt,
        String reviewNote,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
}
