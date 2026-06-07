package com.example.scienceops.admin.adminuser;

import java.util.List;

import com.example.scienceops.common.api.ApiResponse;
import com.example.scienceops.common.api.PagedResponse;
import com.example.scienceops.security.AdminPrincipal;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/admin")
@PreAuthorize("hasAuthority('admin-user:manage')")
public class AdminUserController {

    private final AdminUserService service;

    public AdminUserController(AdminUserService service) {
        this.service = service;
    }

    @GetMapping("/users")
    public ApiResponse<PagedResponse<AdminUserResponse>> list(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String roleCode,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int pageSize
    ) {
        return ApiResponse.ok(service.list(keyword, status, roleCode, page, pageSize));
    }

    @PostMapping("/users")
    public ApiResponse<AdminUserResponse> create(
            @Valid @RequestBody AdminUserRequest request,
            @AuthenticationPrincipal AdminPrincipal principal
    ) {
        return ApiResponse.ok(service.create(request, principal));
    }

    @GetMapping("/users/{adminUserId}")
    public ApiResponse<AdminUserResponse> detail(@PathVariable Long adminUserId) {
        return ApiResponse.ok(service.detail(adminUserId));
    }

    @PutMapping("/users/{adminUserId}")
    public ApiResponse<AdminUserResponse> update(
            @PathVariable Long adminUserId,
            @Valid @RequestBody AdminUserUpdateRequest request,
            @AuthenticationPrincipal AdminPrincipal principal
    ) {
        return ApiResponse.ok(service.update(adminUserId, request, principal));
    }

    @PostMapping("/users/{adminUserId}/reset-password")
    public ApiResponse<AdminUserResponse> resetPassword(
            @PathVariable Long adminUserId,
            @Valid @RequestBody PasswordResetRequest request,
            @AuthenticationPrincipal AdminPrincipal principal
    ) {
        return ApiResponse.ok(service.resetPassword(adminUserId, request, principal));
    }

    @PutMapping("/users/{adminUserId}/roles")
    public ApiResponse<AdminUserResponse> assignRoles(
            @PathVariable Long adminUserId,
            @Valid @RequestBody RoleAssignmentRequest request,
            @AuthenticationPrincipal AdminPrincipal principal
    ) {
        return ApiResponse.ok(service.assignRoles(adminUserId, request, principal));
    }

    @GetMapping("/roles")
    public ApiResponse<List<RoleResponse>> roles() {
        return ApiResponse.ok(service.roles());
    }

    @GetMapping("/permissions")
    public ApiResponse<List<PermissionResponse>> permissions() {
        return ApiResponse.ok(service.permissions());
    }
}
