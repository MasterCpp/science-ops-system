package com.example.scienceops.registration;

import java.time.LocalDateTime;

record RegistrationRecord(
        Long id,
        Long activityId,
        String activityTitle,
        String name,
        String phone,
        int attendeeCount,
        String unitName,
        String ageGroup,
        String remark,
        String status,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
}
