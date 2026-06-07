package com.example.scienceops.survey;

import java.time.LocalDateTime;

public record SurveyResponseRecord(
        Long id,
        Long surveyId,
        Long registrationId,
        String respondentName,
        String respondentPhone,
        LocalDateTime submittedAt,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
}
