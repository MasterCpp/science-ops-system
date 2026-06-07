package com.example.scienceops.survey;

import java.math.BigDecimal;
import java.util.List;

import jakarta.validation.constraints.NotBlank;

public record SurveyAnswerRequest(
        @NotBlank String questionId,
        String optionId,
        List<String> optionIds,
        BigDecimal numericValue,
        String textValue
) {
}
