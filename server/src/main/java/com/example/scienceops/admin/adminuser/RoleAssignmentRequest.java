package com.example.scienceops.admin.adminuser;

import java.util.List;

import jakarta.validation.constraints.NotEmpty;

public record RoleAssignmentRequest(
        @NotEmpty
        List<String> roleCodes
) {
}
