package com.example.scienceops.admin.adminuser;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;
import org.springframework.util.StringUtils;

@Repository
class AdminUserRepository {

    private final JdbcTemplate jdbcTemplate;

    AdminUserRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    void insert(Long id, AdminUserRequest request, String passwordHash, LocalDateTime now) {
        jdbcTemplate.update(
                """
                insert into admin_user (
                  id, username, password_hash, display_name, phone, status,
                  created_at, updated_at, deleted
                ) values (?, ?, ?, ?, ?, ?, ?, ?, 0)
                """,
                id,
                request.username(),
                passwordHash,
                request.displayName(),
                request.phone(),
                request.status(),
                now,
                now
        );
    }

    void update(Long id, AdminUserUpdateRequest request, LocalDateTime now) {
        jdbcTemplate.update(
                """
                update admin_user
                set display_name = ?, phone = ?, status = ?, updated_at = ?
                where id = ? and deleted = 0
                """,
                request.displayName(),
                request.phone(),
                request.status(),
                now,
                id
        );
    }

    void resetPassword(Long id, String passwordHash, LocalDateTime now) {
        jdbcTemplate.update(
                "update admin_user set password_hash = ?, updated_at = ? where id = ? and deleted = 0",
                passwordHash,
                now,
                id
        );
    }

    void deleteUserRoles(Long adminUserId, LocalDateTime now) {
        jdbcTemplate.update(
                "update admin_user_role set deleted = 1, updated_at = ? where admin_user_id = ? and deleted = 0",
                now,
                adminUserId
        );
    }

    void insertUserRole(Long id, Long adminUserId, Long roleId, LocalDateTime now) {
        Integer existing = jdbcTemplate.queryForObject(
                "select count(*) from admin_user_role where admin_user_id = ? and role_id = ?",
                Integer.class,
                adminUserId,
                roleId
        );
        if (existing != null && existing > 0) {
            jdbcTemplate.update(
                    "update admin_user_role set deleted = 0, updated_at = ? where admin_user_id = ? and role_id = ?",
                    now,
                    adminUserId,
                    roleId
            );
            return;
        }
        jdbcTemplate.update(
                """
                insert into admin_user_role (id, admin_user_id, role_id, created_at, updated_at, deleted)
                values (?, ?, ?, ?, ?, 0)
                """,
                id,
                adminUserId,
                roleId,
                now,
                now
        );
    }

    Optional<AdminUserRecord> findById(Long id) {
        return jdbcTemplate.query(
                """
                select id, username, display_name, phone, status, last_login_at, created_at, updated_at
                from admin_user
                where id = ? and deleted = 0
                """,
                this::mapUser,
                id
        ).stream().findFirst();
    }

    Optional<AdminUserRecord> findByUsername(String username) {
        return jdbcTemplate.query(
                """
                select id, username, display_name, phone, status, last_login_at, created_at, updated_at
                from admin_user
                where username = ? and deleted = 0
                """,
                this::mapUser,
                username
        ).stream().findFirst();
    }

    List<AdminUserRecord> list(String keyword, String status, String roleCode, int page, int pageSize) {
        QueryParts query = filteredQuery(keyword, status, roleCode);
        query.sql.append(" order by u.created_at desc, u.id desc limit ? offset ?");
        query.params.add(pageSize);
        query.params.add((page - 1) * pageSize);
        return jdbcTemplate.query(
                """
                select distinct u.id, u.username, u.display_name, u.phone, u.status,
                       u.last_login_at, u.created_at, u.updated_at
                from admin_user u
                """ + query.sql,
                this::mapUser,
                query.params.toArray()
        );
    }

    long count(String keyword, String status, String roleCode) {
        QueryParts query = filteredQuery(keyword, status, roleCode);
        Long count = jdbcTemplate.queryForObject(
                "select count(distinct u.id) from admin_user u" + query.sql,
                Long.class,
                query.params.toArray()
        );
        return count == null ? 0 : count;
    }

    List<RoleRecord> listRoles() {
        return jdbcTemplate.query(
                """
                select id, code, name, description
                from role
                where deleted = 0
                order by code
                """,
                this::mapRole
        );
    }

    List<RoleRecord> listRolesForUser(Long adminUserId) {
        return jdbcTemplate.query(
                """
                select r.id, r.code, r.name, r.description
                from role r
                join admin_user_role aur on aur.role_id = r.id and aur.deleted = 0
                where aur.admin_user_id = ? and r.deleted = 0
                order by r.code
                """,
                this::mapRole,
                adminUserId
        );
    }

    List<RoleRecord> listRolesByCodes(List<String> roleCodes) {
        if (roleCodes == null || roleCodes.isEmpty()) {
            return List.of();
        }
        String placeholders = String.join(",", roleCodes.stream().map(value -> "?").toList());
        return jdbcTemplate.query(
                """
                select id, code, name, description
                from role
                where deleted = 0 and code in (
                """ + placeholders + ") order by code",
                this::mapRole,
                roleCodes.toArray()
        );
    }

    List<PermissionRecord> listPermissions() {
        return jdbcTemplate.query(
                """
                select id, code, name, module, description
                from permission
                where deleted = 0
                order by module, code
                """,
                this::mapPermission
        );
    }

    List<PermissionRecord> listPermissionsForRole(Long roleId) {
        return jdbcTemplate.query(
                """
                select p.id, p.code, p.name, p.module, p.description
                from permission p
                join role_permission rp on rp.permission_id = p.id and rp.deleted = 0
                where rp.role_id = ? and p.deleted = 0
                order by p.module, p.code
                """,
                this::mapPermission,
                roleId
        );
    }

    private QueryParts filteredQuery(String keyword, String status, String roleCode) {
        QueryParts query = new QueryParts();
        if (StringUtils.hasText(roleCode)) {
            query.sql.append(" join admin_user_role aur_filter on aur_filter.admin_user_id = u.id and aur_filter.deleted = 0");
            query.sql.append(" join role r_filter on r_filter.id = aur_filter.role_id and r_filter.deleted = 0");
        }
        query.sql.append(" where u.deleted = 0");
        if (StringUtils.hasText(keyword)) {
            query.sql.append(" and (lower(u.username) like ? or lower(u.display_name) like ? or lower(u.phone) like ?)");
            String pattern = "%" + keyword.toLowerCase() + "%";
            query.params.add(pattern);
            query.params.add(pattern);
            query.params.add(pattern);
        }
        if (StringUtils.hasText(status)) {
            query.sql.append(" and u.status = ?");
            query.params.add(status);
        }
        if (StringUtils.hasText(roleCode)) {
            query.sql.append(" and r_filter.code = ?");
            query.params.add(roleCode);
        }
        return query;
    }

    private AdminUserRecord mapUser(ResultSet resultSet, int rowNumber) throws SQLException {
        return new AdminUserRecord(
                resultSet.getLong("id"),
                resultSet.getString("username"),
                resultSet.getString("display_name"),
                resultSet.getString("phone"),
                resultSet.getString("status"),
                resultSet.getObject("last_login_at", LocalDateTime.class),
                resultSet.getObject("created_at", LocalDateTime.class),
                resultSet.getObject("updated_at", LocalDateTime.class)
        );
    }

    private RoleRecord mapRole(ResultSet resultSet, int rowNumber) throws SQLException {
        return new RoleRecord(
                resultSet.getLong("id"),
                resultSet.getString("code"),
                resultSet.getString("name"),
                resultSet.getString("description")
        );
    }

    private PermissionRecord mapPermission(ResultSet resultSet, int rowNumber) throws SQLException {
        return new PermissionRecord(
                resultSet.getLong("id"),
                resultSet.getString("code"),
                resultSet.getString("name"),
                resultSet.getString("module"),
                resultSet.getString("description")
        );
    }

    private static final class QueryParts {
        private final StringBuilder sql = new StringBuilder();
        private final List<Object> params = new ArrayList<>();
    }
}
