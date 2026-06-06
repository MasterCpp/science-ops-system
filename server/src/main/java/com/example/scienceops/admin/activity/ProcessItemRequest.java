package com.example.scienceops.admin.activity;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record ProcessItemRequest(
        @NotBlank @Size(max = 64) String timeLabel,
        @NotBlank @Size(max = 200) String title,
        String description,
        @NotNull Integer sortOrder
) {
}
