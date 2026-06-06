package com.example.scienceops.volunteer;

import java.time.LocalDateTime;

public record VolunteerAttendanceResponse(
        String id,
        String activityId,
        String activityTitle,
        String applicationId,
        String positionId,
        String positionName,
        String name,
        String phone,
        LocalDateTime checkInTime,
        LocalDateTime checkOutTime,
        Integer serviceMinutes,
        Integer effectiveServiceMinutes,
        String status,
        boolean manuallyAdjusted,
        Integer adjustedServiceMinutes,
        String adjustmentReason,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
}
