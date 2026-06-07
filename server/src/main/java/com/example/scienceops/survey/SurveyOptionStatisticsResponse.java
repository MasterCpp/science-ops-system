package com.example.scienceops.survey;

public record SurveyOptionStatisticsResponse(
        String optionId,
        String label,
        long selectedCount
) {
}
