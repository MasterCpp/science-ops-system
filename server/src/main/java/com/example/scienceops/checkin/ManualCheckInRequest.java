package com.example.scienceops.checkin;

import java.time.LocalDateTime;

import jakarta.validation.constraints.NotBlank;

public record ManualCheckInRequest(
        @NotBlank String registrationId,
        LocalDateTime checkInTime
) {
}
