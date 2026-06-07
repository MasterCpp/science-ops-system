package com.example.scienceops.survey;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record SurveyOptionRequest(
        @NotBlank @Size(max = 255) String label,
        @NotBlank @Size(max = 128) String value,
        Integer sortOrder
) {
}
