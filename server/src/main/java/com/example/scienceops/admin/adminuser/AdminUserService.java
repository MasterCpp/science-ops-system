package com.example.scienceops.admin.adminuser;

import java.time.Clock;
import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import com.baomidou.mybatisplus.core.toolkit.IdWorker;
import com.example.scienceops.common.api.PagedResponse;
import com.example.scienceops.common.error.BusinessRuleException;
import com.example.scienceops.common.error.ConflictException;
import com.example.scienceops.common.error.NotFoundException;
import com.example.scienceops.operationlog.OperationLogService;
import com.example.scienceops.security.AdminPrincipal;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AdminUserService {

    private final AdminUserRepository repository;
    private final PasswordEncoder passwordEncoder;
    private final OperationLogService operationLogService;
    private final Clock clock;

    public AdminUserService(
            AdminUserRepository repository,
            PasswordEncoder passwordEncoder,
            OperationLogService operationLogService
    ) {
        this.repository = repository;
        this.passwordEncoder = passwordEncoder;
        this.operationLogService = operationLogService;
        this.clock = Clock.systemDefaultZone();
    }

    public PagedResponse<AdminUserResponse> list(String keyword, String status, String roleCode, int page, int pageSize) {
        validateStatus(status);
        int safePage = Math.max(page, 1);
        int safePageSize = Math.max(1, Math.min(pageSize, 100));
        return new PagedResponse<>(
                repository.list(keyword, status, roleCode, safePage, safePageSize)
                        .stream()
                        .map(this::toUserResponse)
                        .toList(),
                safePage,
                safePageSize,
                repository.count(keyword, status, roleCode)
        );
    }

    public AdminUserResponse detail(Long adminUserId) {
        return toUserResponse(requireUser(adminUserId));
    }

    @Transactional
    public AdminUserResponse create(AdminUserRequest request, AdminPrincipal principal) {
        validateStatus(request.status());
        if (repository.findByUsername(request.username()).isPresent()) {
            throw new ConflictException("Admin username already exists");
        }
        Long id = IdWorker.getId();
        repository.insert(id, request, passwordEncoder.encode(request.password()), now());
        AdminUserResponse response = detail(id);
        operationLogService.record(principal, "ADMIN_USER_CREATE", "ADMIN_USER", id, request.username(), Map.of(
                "status", request.status()
        ));
        return response;
    }

    @Transactional
    public AdminUserResponse update(Long adminUserId, AdminUserUpdateRequest request, AdminPrincipal principal) {
        validateStatus(request.status());
        AdminUserRecord current = requireUser(adminUserId);
        repository.update(adminUserId, request, now());
        AdminUserResponse response = detail(adminUserId);
        operationLogService.record(principal, "ADMIN_USER_UPDATE", "ADMIN_USER", current.id(), current.username(), updateDetails(current, request));
        return response;
    }

    @Transactional
    public AdminUserResponse resetPassword(Long adminUserId, PasswordResetRequest request, AdminPrincipal principal) {
        AdminUserRecord current = requireUser(adminUserId);
        repository.resetPassword(adminUserId, passwordEncoder.encode(request.password()), now());
        AdminUserResponse response = detail(adminUserId);
        operationLogService.record(principal, "ADMIN_USER_RESET_PASSWORD", "ADMIN_USER", current.id(), current.username(), Map.of());
        return response;
    }

    @Transactional
    public AdminUserResponse assignRoles(Long adminUserId, RoleAssignmentRequest request, AdminPrincipal principal) {
        AdminUserRecord current = requireUser(adminUserId);
        List<String> roleCodes = request.roleCodes().stream()
                .filter(code -> code != null && !code.isBlank())
                .distinct()
                .toList();
        if (roleCodes.isEmpty()) {
            throw new BusinessRuleException("INVALID_REQUEST", "At least one role is required", 400);
        }

        List<RoleRecord> roles = repository.listRolesByCodes(roleCodes);
        Set<String> foundCodes = roles.stream().map(RoleRecord::code).collect(Collectors.toSet());
        List<String> missingCodes = roleCodes.stream().filter(code -> !foundCodes.contains(code)).toList();
        if (!missingCodes.isEmpty()) {
            throw new NotFoundException("Role not found: " + missingCodes.get(0));
        }

        LocalDateTime now = now();
        repository.deleteUserRoles(adminUserId, now);
        for (RoleRecord role : roles) {
            repository.insertUserRole(IdWorker.getId(), adminUserId, role.id(), now);
        }
        AdminUserResponse response = detail(adminUserId);
        operationLogService.record(principal, "ADMIN_USER_ASSIGN_ROLES", "ADMIN_USER", current.id(), current.username(), Map.of(
                "roleCodes", roleCodes
        ));
        return response;
    }

    public List<RoleResponse> roles() {
        return repository.listRoles().stream()
                .map(this::toRoleResponse)
                .toList();
    }

    public List<PermissionResponse> permissions() {
        return repository.listPermissions().stream()
                .map(this::toPermissionResponse)
                .toList();
    }

    private AdminUserRecord requireUser(Long adminUserId) {
        return repository.findById(adminUserId)
                .orElseThrow(() -> new NotFoundException("Admin user not found"));
    }

    private AdminUserResponse toUserResponse(AdminUserRecord user) {
        return new AdminUserResponse(
                String.valueOf(user.id()),
                user.username(),
                user.displayName(),
                user.phone(),
                user.status(),
                user.lastLoginAt(),
                repository.listRolesForUser(user.id()).stream().map(this::toRoleResponse).toList(),
                user.createdAt(),
                user.updatedAt()
        );
    }

    private RoleResponse toRoleResponse(RoleRecord role) {
        return new RoleResponse(
                String.valueOf(role.id()),
                role.code(),
                role.name(),
                role.description(),
                repository.listPermissionsForRole(role.id()).stream().map(this::toPermissionResponse).toList()
        );
    }

    private PermissionResponse toPermissionResponse(PermissionRecord permission) {
        return new PermissionResponse(
                String.valueOf(permission.id()),
                permission.code(),
                permission.name(),
                permission.module(),
                permission.description()
        );
    }

    private void validateStatus(String status) {
        if (status != null && !Set.of("ENABLED", "DISABLED").contains(status)) {
            throw new BusinessRuleException("INVALID_REQUEST", "Admin user status is not supported", 400);
        }
    }

    private Map<String, Object> updateDetails(AdminUserRecord current, AdminUserUpdateRequest request) {
        Map<String, Object> details = new LinkedHashMap<>();
        details.put("fromStatus", current.status());
        details.put("toStatus", request.status());
        details.put("displayName", request.displayName());
        details.put("phone", request.phone());
        return details;
    }

    private LocalDateTime now() {
        return LocalDateTime.now(clock);
    }
}
