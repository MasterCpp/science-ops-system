package com.example.scienceops.volunteer;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record VolunteerAttendanceAdjustRequest(
        @NotNull @Min(0) Integer serviceMinutes,
        @NotBlank @Size(max = 500) String adjustmentReason
) {
}
