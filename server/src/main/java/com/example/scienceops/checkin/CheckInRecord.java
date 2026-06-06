package com.example.scienceops.checkin;

import java.time.LocalDateTime;

record CheckInRecord(
        Long id,
        Long activityId,
        String activityTitle,
        Long registrationId,
        String name,
        String phone,
        LocalDateTime checkInTime,
        String method,
        boolean manual,
        String status
) {
}
