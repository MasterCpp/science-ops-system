package com.example.scienceops.survey;

import java.time.LocalDateTime;

public record SurveyOptionRecord(
        Long id,
        Long questionId,
        String label,
        String value,
        Integer sortOrder,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
}
