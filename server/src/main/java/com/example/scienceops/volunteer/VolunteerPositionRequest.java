package com.example.scienceops.volunteer;

import java.time.LocalDateTime;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record VolunteerPositionRequest(
        @NotBlank @Size(max = 128) String name,
        String description,
        @NotNull @Min(1) Integer capacity,
        @NotNull LocalDateTime serviceStartTime,
        @NotNull LocalDateTime serviceEndTime
) {
}
