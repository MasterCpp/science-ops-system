package com.example.scienceops.survey;

import java.math.BigDecimal;
import java.util.List;

public record SurveyRawAnswerResponse(
        String questionId,
        String questionTitle,
        String questionType,
        String optionId,
        String optionLabel,
        List<String> optionIds,
        BigDecimal numericValue,
        String textValue
) {
}
