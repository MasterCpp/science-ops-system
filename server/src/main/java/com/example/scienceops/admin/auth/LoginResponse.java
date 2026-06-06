package com.example.scienceops.admin.auth;

public record LoginResponse(
        String token,
        AdminProfileResponse admin
) {
}
