package com.example.scienceops.admin.activity;

import java.util.List;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record CustomFieldRequest(
        @NotBlank @Size(max = 64) @Pattern(regexp = "[a-zA-Z][a-zA-Z0-9_]*") String fieldKey,
        @NotBlank @Size(max = 128) String label,
        @NotBlank @Pattern(regexp = "TEXT|SELECT|MULTI_SELECT|NUMBER") String fieldType,
        @NotNull Boolean required,
        List<String> options,
        @NotNull Integer sortOrder
) {
}
