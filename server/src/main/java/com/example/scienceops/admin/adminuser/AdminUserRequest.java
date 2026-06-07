package com.example.scienceops.admin.adminuser;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record AdminUserRequest(
        @NotBlank
        @Size(max = 64)
        String username,

        @NotBlank
        @Size(max = 64)
        String displayName,

        @Size(max = 32)
        String phone,

        @NotBlank
        @Pattern(regexp = "ENABLED|DISABLED")
        String status,

        @NotBlank
        @Size(min = 6, max = 64)
        String password
) {
}
