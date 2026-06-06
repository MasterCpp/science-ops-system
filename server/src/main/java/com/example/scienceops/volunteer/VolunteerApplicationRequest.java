package com.example.scienceops.volunteer;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record VolunteerApplicationRequest(
        @NotBlank String positionId,
        @NotBlank @Size(max = 64) String name,
        @NotBlank @Size(max = 32) String phone,
        @Size(max = 128) String unitName,
        @Size(max = 64) String ageGroup,
        @Size(max = 500) String availableTimeNote,
        @Size(max = 500) String experienceNote,
        @Size(max = 500) String remark
) {
}
