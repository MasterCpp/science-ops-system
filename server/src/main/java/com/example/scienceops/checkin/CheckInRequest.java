package com.example.scienceops.checkin;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CheckInRequest(
        @NotBlank @Size(max = 32) String phone
) {
}
