package com.example.scienceops.security;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.LinkedHashSet;
import java.util.Optional;
import java.util.Set;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
public class AdminAuthRepository {

    private final JdbcTemplate jdbcTemplate;

    public AdminAuthRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public Optional<AdminAccountRecord> findAccountByUsername(String username) {
        return jdbcTemplate.query(
                """
                select id, username, password_hash, display_name, status
                from admin_user
                where username = ? and deleted = 0
                """,
                this::mapAccount,
                username
        ).stream().findFirst();
    }

    public AdminPrincipal loadPrincipal(String username) {
        AdminAccountRecord account = findAccountByUsername(username)
                .orElseThrow(() -> new IllegalArgumentException("Admin account not found"));
        return new AdminPrincipal(
                account.id(),
                account.username(),
                account.displayName(),
                account.status(),
                loadRoles(account.id()),
                loadPermissions(account.id())
        );
    }

    public Set<String> loadRoles(Long adminUserId) {
        return new LinkedHashSet<>(jdbcTemplate.queryForList(
                """
                select r.code
                from role r
                join admin_user_role aur on aur.role_id = r.id and aur.deleted = 0
                where aur.admin_user_id = ? and r.deleted = 0
                order by r.code
                """,
                String.class,
                adminUserId
        ));
    }

    public Set<String> loadPermissions(Long adminUserId) {
        return new LinkedHashSet<>(jdbcTemplate.queryForList(
                """
                select distinct p.code
                from permission p
                join role_permission rp on rp.permission_id = p.id and rp.deleted = 0
                join admin_user_role aur on aur.role_id = rp.role_id and aur.deleted = 0
                where aur.admin_user_id = ? and p.deleted = 0
                order by p.code
                """,
                String.class,
                adminUserId
        ));
    }

    private AdminAccountRecord mapAccount(ResultSet resultSet, int rowNumber) throws SQLException {
        return new AdminAccountRecord(
                resultSet.getLong("id"),
                resultSet.getString("username"),
                resultSet.getString("password_hash"),
                resultSet.getString("display_name"),
                resultSet.getString("status")
        );
    }

    public record AdminAccountRecord(
            Long id,
            String username,
            String passwordHash,
            String displayName,
            String status
    ) {
    }
}
