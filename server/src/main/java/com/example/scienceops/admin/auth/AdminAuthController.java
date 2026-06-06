package com.example.scienceops.admin.auth;

import com.example.scienceops.common.api.ApiResponse;
import com.example.scienceops.security.AdminPrincipal;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/admin/auth")
public class AdminAuthController {

    private final AdminAuthService authService;

    public AdminAuthController(AdminAuthService authService) {
        this.authService = authService;
    }

    @PostMapping("/login")
    public ResponseEntity<ApiResponse<LoginResponse>> login(@Valid @RequestBody LoginRequest request) {
        return ResponseEntity.ok(ApiResponse.ok(authService.login(request.username(), request.password())));
    }

    @GetMapping("/me")
    public ResponseEntity<ApiResponse<AdminProfileResponse>> me(@AuthenticationPrincipal AdminPrincipal principal) {
        return ResponseEntity.ok(ApiResponse.ok(AdminProfileResponse.from(principal)));
    }
}
