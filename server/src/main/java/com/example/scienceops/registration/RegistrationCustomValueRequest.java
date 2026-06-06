package com.example.scienceops.registration;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record RegistrationCustomValueRequest(
        @NotBlank @Size(max = 64) String fieldKey,
        String value
) {
}
