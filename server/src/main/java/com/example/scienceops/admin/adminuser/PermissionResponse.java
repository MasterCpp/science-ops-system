package com.example.scienceops.admin.adminuser;

public record PermissionResponse(
        String id,
        String code,
        String name,
        String module,
        String description
) {
}
