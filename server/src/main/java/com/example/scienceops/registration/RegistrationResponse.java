package com.example.scienceops.registration;

import java.time.LocalDateTime;
import java.util.List;

public record RegistrationResponse(
        String id,
        String activityId,
        String activityTitle,
        String name,
        String phone,
        int attendeeCount,
        String unitName,
        String ageGroup,
        String remark,
        String status,
        List<RegistrationCustomValueResponse> customValues,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
}
