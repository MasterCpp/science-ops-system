package com.example.scienceops.admin.adminuser;

import java.time.LocalDateTime;

record AdminUserRecord(
        Long id,
        String username,
        String displayName,
        String phone,
        String status,
        LocalDateTime lastLoginAt,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
}
