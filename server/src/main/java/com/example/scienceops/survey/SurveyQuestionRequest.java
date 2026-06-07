package com.example.scienceops.survey;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record SurveyQuestionRequest(
        @NotBlank @Size(max = 500) String title,
        @NotBlank String type,
        @NotNull Boolean required,
        Integer sortOrder
) {
}
