package com.example.scienceops.survey;

import java.time.LocalDateTime;

public record SurveyRecord(
        Long id,
        Long activityId,
        String title,
        String description,
        String status,
        Long createdBy,
        Long updatedBy,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
}
