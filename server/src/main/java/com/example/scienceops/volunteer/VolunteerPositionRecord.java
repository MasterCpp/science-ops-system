package com.example.scienceops.volunteer;

import java.time.LocalDateTime;

record VolunteerPositionRecord(
        Long id,
        Long activityId,
        String activityTitle,
        String name,
        String description,
        Integer capacity,
        Long approvedCount,
        LocalDateTime serviceStartTime,
        LocalDateTime serviceEndTime,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
}
