package com.example.scienceops.survey;

import java.time.LocalDateTime;

public record SurveySubmitResponse(
        String id,
        String surveyId,
        String registrationId,
        String respondentName,
        String respondentPhone,
        LocalDateTime submittedAt
) {
}
