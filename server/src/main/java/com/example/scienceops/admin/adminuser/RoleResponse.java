package com.example.scienceops.admin.adminuser;

import java.util.List;

public record RoleResponse(
        String id,
        String code,
        String name,
        String description,
        List<PermissionResponse> permissions
) {
}
