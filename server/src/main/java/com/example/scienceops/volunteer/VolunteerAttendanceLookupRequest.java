package com.example.scienceops.volunteer;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record VolunteerAttendanceLookupRequest(
        @NotBlank @Size(max = 32) String phone
) {
}
