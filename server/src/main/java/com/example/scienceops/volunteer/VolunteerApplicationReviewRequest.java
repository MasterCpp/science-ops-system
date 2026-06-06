package com.example.scienceops.volunteer;

import jakarta.validation.constraints.Size;

public record VolunteerApplicationReviewRequest(
        @Size(max = 500) String reviewNote
) {
}
