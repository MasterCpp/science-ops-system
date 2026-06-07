package com.example.scienceops.survey;

import java.time.LocalDateTime;
import java.util.List;

public record SurveyResponse(
        String id,
        String activityId,
        String title,
        String description,
        String status,
        List<SurveyQuestionResponse> questions,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
}
