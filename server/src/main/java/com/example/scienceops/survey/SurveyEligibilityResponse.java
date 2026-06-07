package com.example.scienceops.survey;

public record SurveyEligibilityResponse(
        boolean eligible,
        String surveyId,
        String registrationId,
        String name,
        String phone,
        String status
) {
}
