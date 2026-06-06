package com.example.scienceops.visitorreport;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;
import org.springframework.util.StringUtils;

@Repository
class VisitorReportRepository {

    private final JdbcTemplate jdbcTemplate;

    VisitorReportRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    boolean activityExists(Long activityId) {
        Long count = jdbcTemplate.queryForObject(
                "select count(*) from activity where id = ? and deleted = 0",
                Long.class,
                activityId
        );
        return count != null && count > 0;
    }

    void insert(Long id, VisitorReportRequest request, Long adminUserId, LocalDateTime now) {
        jdbcTemplate.update(
                """
                        insert into visitor_report
                          (id, activity_id, visitor_unit, contact_name, contact_phone, visitor_count,
                           visit_date, visit_reason, remark, created_by, updated_by, created_at, updated_at, deleted)
                        values (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, 0)
                        """,
                id,
                request.activityId(),
                request.visitorUnit(),
                request.contactName(),
                request.contactPhone(),
                request.visitorCount(),
                request.visitDate(),
                request.visitReason(),
                request.remark(),
                adminUserId,
                adminUserId,
                now,
                now
        );
    }

    void update(Long id, VisitorReportRequest request, Long adminUserId, LocalDateTime now) {
        jdbcTemplate.update(
                """
                        update visitor_report
                        set activity_id = ?,
                            visitor_unit = ?,
                            contact_name = ?,
                            contact_phone = ?,
                            visitor_count = ?,
                            visit_date = ?,
                            visit_reason = ?,
                            remark = ?,
                            updated_by = ?,
                            updated_at = ?
                        where id = ? and deleted = 0
                        """,
                request.activityId(),
                request.visitorUnit(),
                request.contactName(),
                request.contactPhone(),
                request.visitorCount(),
                request.visitDate(),
                request.visitReason(),
                request.remark(),
                adminUserId,
                now,
                id
        );
    }

    void delete(Long id, Long adminUserId, LocalDateTime now) {
        jdbcTemplate.update(
                "update visitor_report set deleted = 1, updated_by = ?, updated_at = ? where id = ? and deleted = 0",
                adminUserId,
                now,
                id
        );
    }

    Optional<VisitorReportRecord> findById(Long id) {
        return jdbcTemplate.query(
                selectSql() + " where vr.id = ? and vr.deleted = 0",
                this::mapRecord,
                id
        ).stream().findFirst();
    }

    List<VisitorReportRecord> list(String keyword, Long activityId, LocalDate visitFrom, LocalDate visitTo, int page, int pageSize) {
        QueryParts query = filteredQuery(keyword, activityId, visitFrom, visitTo);
        query.sql.append(" order by vr.visit_date desc, vr.id desc limit ? offset ?");
        query.params.add(pageSize);
        query.params.add((page - 1) * pageSize);
        return jdbcTemplate.query(selectSql() + query.sql, this::mapRecord, query.params.toArray());
    }

    long count(String keyword, Long activityId, LocalDate visitFrom, LocalDate visitTo) {
        QueryParts query = filteredQuery(keyword, activityId, visitFrom, visitTo);
        Long count = jdbcTemplate.queryForObject(
                "select count(*) from visitor_report vr" + query.sql,
                Long.class,
                query.params.toArray()
        );
        return count == null ? 0 : count;
    }

    private QueryParts filteredQuery(String keyword, Long activityId, LocalDate visitFrom, LocalDate visitTo) {
        QueryParts query = new QueryParts();
        query.sql.append(" where vr.deleted = 0");
        if (StringUtils.hasText(keyword)) {
            query.sql.append(" and (lower(vr.visitor_unit) like ? or lower(vr.contact_name) like ? or lower(vr.contact_phone) like ? or lower(vr.visit_reason) like ?)");
            String pattern = "%" + keyword.toLowerCase() + "%";
            query.params.add(pattern);
            query.params.add(pattern);
            query.params.add(pattern);
            query.params.add(pattern);
        }
        if (activityId != null) {
            query.sql.append(" and vr.activity_id = ?");
            query.params.add(activityId);
        }
        if (visitFrom != null) {
            query.sql.append(" and vr.visit_date >= ?");
            query.params.add(visitFrom.atStartOfDay());
        }
        if (visitTo != null) {
            query.sql.append(" and vr.visit_date < ?");
            query.params.add(visitTo.plusDays(1).atStartOfDay());
        }
        return query;
    }

    private String selectSql() {
        return """
                select vr.id,
                       vr.activity_id,
                       a.title as activity_title,
                       vr.visitor_unit,
                       vr.contact_name,
                       vr.contact_phone,
                       vr.visitor_count,
                       vr.visit_date,
                       vr.visit_reason,
                       vr.remark,
                       vr.created_by,
                       vr.updated_by,
                       vr.created_at,
                       vr.updated_at
                from visitor_report vr
                left join activity a on a.id = vr.activity_id and a.deleted = 0
                """;
    }

    private VisitorReportRecord mapRecord(ResultSet resultSet, int rowNumber) throws SQLException {
        return new VisitorReportRecord(
                resultSet.getLong("id"),
                getNullableLong(resultSet, "activity_id"),
                resultSet.getString("activity_title"),
                resultSet.getString("visitor_unit"),
                resultSet.getString("contact_name"),
                resultSet.getString("contact_phone"),
                resultSet.getInt("visitor_count"),
                resultSet.getObject("visit_date", LocalDateTime.class),
                resultSet.getString("visit_reason"),
                resultSet.getString("remark"),
                getNullableLong(resultSet, "created_by"),
                getNullableLong(resultSet, "updated_by"),
                resultSet.getObject("created_at", LocalDateTime.class),
                resultSet.getObject("updated_at", LocalDateTime.class)
        );
    }

    private Long getNullableLong(ResultSet resultSet, String column) throws SQLException {
        long value = resultSet.getLong(column);
        return resultSet.wasNull() ? null : value;
    }

    private static final class QueryParts {
        private final StringBuilder sql = new StringBuilder();
        private final List<Object> params = new ArrayList<>();
    }
}
