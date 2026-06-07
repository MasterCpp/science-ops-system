package com.example.scienceops.admin.adminuser;

import java.time.LocalDateTime;
import java.util.List;

public record AdminUserResponse(
        String id,
        String username,
        String displayName,
        String phone,
        String status,
        LocalDateTime lastLoginAt,
        List<RoleResponse> roles,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
}
