package com.example.scienceops.admin.adminuser;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record PasswordResetRequest(
        @NotBlank
        @Size(min = 6, max = 64)
        String password
) {
}
