package com.example.scienceops.volunteer;

import java.time.LocalDateTime;

record VolunteerAttendanceRecord(
        Long id,
        Long activityId,
        String activityTitle,
        Long applicationId,
        Long positionId,
        String positionName,
        String name,
        String phone,
        LocalDateTime checkInTime,
        LocalDateTime checkOutTime,
        Integer serviceMinutes,
        String status,
        boolean manuallyAdjusted,
        Integer adjustedServiceMinutes,
        String adjustmentReason,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
}
