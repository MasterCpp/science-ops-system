package com.example.scienceops.survey;

import java.util.List;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record SurveySubmitRequest(
        @NotBlank String phone,
        @NotNull @Valid List<SurveyAnswerRequest> answers
) {
}
