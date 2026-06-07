package com.example.scienceops.survey;

import java.math.BigDecimal;

public record SurveyAnswerRecord(
        Long responseId,
        Long questionId,
        String questionTitle,
        String questionType,
        Long optionId,
        String optionLabel,
        String optionIdsJson,
        BigDecimal numericValue,
        String textValue
) {
}
