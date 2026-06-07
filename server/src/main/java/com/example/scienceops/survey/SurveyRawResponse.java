package com.example.scienceops.survey;

import java.time.LocalDateTime;
import java.util.List;

public record SurveyRawResponse(
        String id,
        String surveyId,
        String registrationId,
        String respondentName,
        String respondentPhone,
        LocalDateTime submittedAt,
        List<SurveyRawAnswerResponse> answers
) {
}
