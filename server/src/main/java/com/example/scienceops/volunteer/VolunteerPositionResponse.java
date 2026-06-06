package com.example.scienceops.volunteer;

import java.time.LocalDateTime;

public record VolunteerPositionResponse(
        String id,
        String activityId,
        String activityTitle,
        String name,
        String description,
        Integer capacity,
        Long approvedCount,
        Integer remainingCapacity,
        boolean full,
        LocalDateTime serviceStartTime,
        LocalDateTime serviceEndTime,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
}
