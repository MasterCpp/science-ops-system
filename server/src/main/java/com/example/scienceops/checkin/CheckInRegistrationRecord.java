package com.example.scienceops.checkin;

record CheckInRegistrationRecord(
        Long id,
        Long activityId,
        String activityTitle,
        String name,
        String phone,
        String status
) {
}
