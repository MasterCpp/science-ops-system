package com.example.scienceops.operationlog;

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
class OperationLogRepository {

    private final JdbcTemplate jdbcTemplate;

    OperationLogRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    void insert(
            Long id,
            Long adminUserId,
            String adminUsername,
            String adminRoleCode,
            String action,
            String targetType,
            Long targetId,
            String targetSummary,
            String ip,
            String userAgent,
            String detailJson,
            LocalDateTime createdAt
    ) {
        jdbcTemplate.update(
                """
                insert into operation_log (
                  id, admin_user_id, admin_username, admin_role_code, action,
                  target_type, target_id, target_summary, ip, user_agent, detail_json, created_at
                ) values (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                """,
                id,
                adminUserId,
                adminUsername,
                adminRoleCode,
                action,
                targetType,
                targetId,
                targetSummary,
                ip,
                userAgent,
                detailJson,
                createdAt
        );
    }

    Optional<OperationLogRecord> findById(Long id) {
        return jdbcTemplate.query(
                """
                select id, admin_user_id, admin_username, admin_role_code, action,
                       target_type, target_id, target_summary, ip, user_agent, detail_json, created_at
                from operation_log
                where id = ?
                """,
                this::mapRecord,
                id
        ).stream().findFirst();
    }

    List<OperationLogRecord> list(
            Long adminUserId,
            String action,
            String targetType,
            LocalDateTime createdFrom,
            LocalDateTime createdTo,
            int page,
            int pageSize
    ) {
        QueryParts query = filteredQuery(adminUserId, action, targetType, createdFrom, createdTo);
        query.sql.append(" order by created_at desc, id desc limit ? offset ?");
        query.params.add(pageSize);
        query.params.add((page - 1) * pageSize);
        return jdbcTemplate.query(
                """
                select id, admin_user_id, admin_username, admin_role_code, action,
                       target_type, target_id, target_summary, ip, user_agent, detail_json, created_at
                from operation_log
                """ + query.sql,
                this::mapRecord,
                query.params.toArray()
        );
    }

    long count(Long adminUserId, String action, String targetType, LocalDateTime createdFrom, LocalDateTime createdTo) {
        QueryParts query = filteredQuery(adminUserId, action, targetType, createdFrom, createdTo);
        Long count = jdbcTemplate.queryForObject(
                "select count(*) from operation_log" + query.sql,
                Long.class,
                query.params.toArray()
        );
        return count == null ? 0 : count;
    }

    private QueryParts filteredQuery(
            Long adminUserId,
            String action,
            String targetType,
            LocalDateTime createdFrom,
            LocalDateTime createdTo
    ) {
        QueryParts query = new QueryParts();
        query.sql.append(" where 1 = 1");
        if (adminUserId != null) {
            query.sql.append(" and admin_user_id = ?");
            query.params.add(adminUserId);
        }
        if (StringUtils.hasText(action)) {
            query.sql.append(" and action = ?");
            query.params.add(action);
        }
        if (StringUtils.hasText(targetType)) {
            query.sql.append(" and target_type = ?");
            query.params.add(targetType);
        }
        if (createdFrom != null) {
            query.sql.append(" and created_at >= ?");
            query.params.add(createdFrom);
        }
        if (createdTo != null) {
            query.sql.append(" and created_at <= ?");
            query.params.add(createdTo);
        }
        return query;
    }

    private OperationLogRecord mapRecord(ResultSet resultSet, int rowNumber) throws SQLException {
        long adminUserId = resultSet.getLong("admin_user_id");
        boolean adminUserIdWasNull = resultSet.wasNull();
        long targetId = resultSet.getLong("target_id");
        boolean targetIdWasNull = resultSet.wasNull();
        return new OperationLogRecord(
                resultSet.getLong("id"),
                adminUserIdWasNull ? null : adminUserId,
                resultSet.getString("admin_username"),
                resultSet.getString("admin_role_code"),
                resultSet.getString("action"),
                resultSet.getString("target_type"),
                targetIdWasNull ? null : targetId,
                resultSet.getString("target_summary"),
                resultSet.getString("ip"),
                resultSet.getString("user_agent"),
                resultSet.getString("detail_json"),
                resultSet.getObject("created_at", LocalDateTime.class)
        );
    }

    private static final class QueryParts {
        private final StringBuilder sql = new StringBuilder();
        private final List<Object> params = new ArrayList<>();
    }
}
