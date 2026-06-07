package com.example.scienceops.survey;

import java.math.BigDecimal;
import java.util.List;

public record SurveyQuestionStatisticsResponse(
        String questionId,
        String title,
        String type,
        long answerCount,
        BigDecimal averageRating,
        List<SurveyOptionStatisticsResponse> options
) {
}
