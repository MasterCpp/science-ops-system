package com.example.scienceops.survey;

import java.time.LocalDateTime;

public record SurveyQuestionRecord(
        Long id,
        Long surveyId,
        String title,
        String type,
        Boolean required,
        Integer sortOrder,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
}
