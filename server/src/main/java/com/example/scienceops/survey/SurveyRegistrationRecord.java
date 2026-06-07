package com.example.scienceops.survey;

public record SurveyRegistrationRecord(
        Long id,
        Long activityId,
        String name,
        String phone,
        String status,
        String checkInStatus
) {
}
