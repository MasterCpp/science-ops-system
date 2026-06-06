package com.example.scienceops.volunteer;

import java.time.LocalDateTime;

import jakarta.validation.constraints.NotBlank;

public record ManualVolunteerCheckInRequest(
        @NotBlank String applicationId,
        LocalDateTime checkInTime
) {
}
