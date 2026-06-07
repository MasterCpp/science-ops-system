package com.example.scienceops.survey;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record SurveyRequest(
        @NotBlank @Size(max = 200) String title,
        String description
) {
}
