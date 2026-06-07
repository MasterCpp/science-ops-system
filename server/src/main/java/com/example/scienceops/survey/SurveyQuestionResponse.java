package com.example.scienceops.survey;

import java.time.LocalDateTime;
import java.util.List;

public record SurveyQuestionResponse(
        String id,
        String surveyId,
        String title,
        String type,
        Boolean required,
        Integer sortOrder,
        List<SurveyOptionResponse> options,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
}
