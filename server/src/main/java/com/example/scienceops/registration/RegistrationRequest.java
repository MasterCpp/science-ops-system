package com.example.scienceops.registration;

import java.util.List;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record RegistrationRequest(
        @NotBlank @Size(max = 64) String name,
        @NotBlank @Size(max = 32) String phone,
        @NotNull @Min(1) Integer attendeeCount,
        @Size(max = 128) String unitName,
        @Size(max = 64) String ageGroup,
        @Size(max = 500) String remark,
        List<@Valid RegistrationCustomValueRequest> customValues
) {
}
