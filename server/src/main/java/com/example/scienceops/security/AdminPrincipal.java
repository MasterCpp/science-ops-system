package com.example.scienceops.security;

import java.util.Set;

public record AdminPrincipal(
        Long id,
        String username,
        String displayName,
        String status,
        Set<String> roles,
        Set<String> permissions
) {
}
