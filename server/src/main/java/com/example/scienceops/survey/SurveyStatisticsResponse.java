package com.example.scienceops.survey;

import java.math.BigDecimal;
import java.util.List;

public record SurveyStatisticsResponse(
        String surveyId,
        long responseCount,
        BigDecimal averageRating,
        List<SurveyQuestionStatisticsResponse> questions
) {
}
