package com.example.scienceops.admin.auth;

import java.util.Set;

import com.example.scienceops.security.AdminPrincipal;

public record AdminProfileResponse(
        String id,
        String username,
        String displayName,
        Set<String> roles,
        Set<String> permissions
) {

    public static AdminProfileResponse from(AdminPrincipal principal) {
        return new AdminProfileResponse(
                principal.id().toString(),
                principal.username(),
                principal.displayName(),
                principal.roles(),
                principal.permissions()
        );
    }
}
