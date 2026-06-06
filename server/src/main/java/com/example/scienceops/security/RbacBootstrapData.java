package com.example.scienceops.security;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

@Component
public class RbacBootstrapData implements ApplicationRunner {

    public static final String DEFAULT_PASSWORD = "password123";

    private final JdbcTemplate jdbcTemplate;
    private final PasswordEncoder passwordEncoder;

    public RbacBootstrapData(JdbcTemplate jdbcTemplate, PasswordEncoder passwordEncoder) {
        this.jdbcTemplate = jdbcTemplate;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    public void run(ApplicationArguments args) {
        Integer existingUsers = jdbcTemplate.queryForObject("select count(*) from admin_user", Integer.class);
        if (existingUsers != null && existingUsers > 0) {
            return;
        }

        LocalDateTime now = LocalDateTime.now();
        insertRoles(now);
        insertPermissions(now);
        insertRolePermissions(now);
        insertUsers(now);
        insertUserRoles(now);
    }

    private void insertRoles(LocalDateTime now) {
        insertRole(1001L, "SUPER_ADMIN", "Super Admin", "Full system access", now);
        insertRole(1002L, "ACTIVITY_ADMIN", "Activity Admin", "Activity operation access", now);
        insertRole(1003L, "VOLUNTEER_ADMIN", "Volunteer Admin", "Volunteer operation access", now);
    }

    private void insertPermissions(LocalDateTime now) {
        insertPermission(2001L, "admin-user:manage", "Manage admin users", "account", now);
        insertPermission(2002L, "operation-log:view", "View operation logs", "audit", now);
        insertPermission(2003L, "activity:manage", "Manage activities", "activity", now);
        insertPermission(2004L, "registration:manage", "Manage registrations", "registration", now);
        insertPermission(2005L, "check-in:manage", "Manage check-ins", "check-in", now);
        insertPermission(2006L, "volunteer:manage", "Manage volunteers", "volunteer", now);
        insertPermission(2007L, "visitor-report:manage", "Manage visitor reports", "visitor-report", now);
        insertPermission(2008L, "survey:manage", "Manage surveys", "survey", now);
        insertPermission(2009L, "file:manage", "Manage files", "file", now);
    }

    private void insertRolePermissions(LocalDateTime now) {
        Map<Long, List<Long>> permissionsByRole = Map.of(
                1001L, List.of(2001L, 2002L, 2003L, 2004L, 2005L, 2006L, 2007L, 2008L, 2009L),
                1002L, List.of(2003L, 2004L, 2005L, 2007L, 2008L, 2009L),
                1003L, List.of(2006L)
        );
        long id = 3001L;
        for (Map.Entry<Long, List<Long>> entry : permissionsByRole.entrySet()) {
            for (Long permissionId : entry.getValue()) {
                jdbcTemplate.update(
                        "insert into role_permission (id, role_id, permission_id, created_at, updated_at, deleted) values (?, ?, ?, ?, ?, 0)",
                        id++,
                        entry.getKey(),
                        permissionId,
                        now,
                        now
                );
            }
        }
    }

    private void insertUsers(LocalDateTime now) {
        String hash = passwordEncoder.encode(DEFAULT_PASSWORD);
        insertUser(4001L, "superadmin", hash, "Super Admin", "ENABLED", now);
        insertUser(4002L, "activityadmin", hash, "Activity Admin", "ENABLED", now);
        insertUser(4003L, "volunteeradmin", hash, "Volunteer Admin", "ENABLED", now);
        insertUser(4004L, "disabledadmin", hash, "Disabled Admin", "DISABLED", now);
    }

    private void insertUserRoles(LocalDateTime now) {
        insertUserRole(5001L, 4001L, 1001L, now);
        insertUserRole(5002L, 4002L, 1002L, now);
        insertUserRole(5003L, 4003L, 1003L, now);
        insertUserRole(5004L, 4004L, 1001L, now);
    }

    private void insertRole(Long id, String code, String name, String description, LocalDateTime now) {
        jdbcTemplate.update(
                "insert into role (id, code, name, description, created_at, updated_at, deleted) values (?, ?, ?, ?, ?, ?, 0)",
                id,
                code,
                name,
                description,
                now,
                now
        );
    }

    private void insertPermission(Long id, String code, String name, String module, LocalDateTime now) {
        jdbcTemplate.update(
                "insert into permission (id, code, name, module, created_at, updated_at, deleted) values (?, ?, ?, ?, ?, ?, 0)",
                id,
                code,
                name,
                module,
                now,
                now
        );
    }

    private void insertUser(Long id, String username, String hash, String displayName, String status, LocalDateTime now) {
        jdbcTemplate.update(
                "insert into admin_user (id, username, password_hash, display_name, status, created_at, updated_at, deleted) values (?, ?, ?, ?, ?, ?, ?, 0)",
                id,
                username,
                hash,
                displayName,
                status,
                now,
                now
        );
    }

    private void insertUserRole(Long id, Long adminUserId, Long roleId, LocalDateTime now) {
        jdbcTemplate.update(
                "insert into admin_user_role (id, admin_user_id, role_id, created_at, updated_at, deleted) values (?, ?, ?, ?, ?, 0)",
                id,
                adminUserId,
                roleId,
                now,
                now
        );
    }
}
