package com.example.scienceops.checkin;

import java.time.LocalDateTime;

public record CheckInResponse(
        String id,
        String activityId,
        String activityTitle,
        String registrationId,
        String name,
        String phone,
        LocalDateTime checkInTime,
        String method,
        boolean manual,
        String status
) {
}
