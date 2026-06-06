package com.example.scienceops.checkin;

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
class CheckInRepository {

    private final JdbcTemplate jdbcTemplate;

    CheckInRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    Optional<CheckInActivityRecord> findActivity(Long activityId) {
        return jdbcTemplate.query(
                """
                        select id, title, status
                        from activity
                        where id = ? and deleted = 0
                        """,
                (resultSet, rowNumber) -> new CheckInActivityRecord(
                        resultSet.getLong("id"),
                        resultSet.getString("title"),
                        resultSet.getString("status")
                ),
                activityId
        ).stream().findFirst();
    }

    Optional<CheckInRegistrationRecord> findRegistrationByPhone(Long activityId, String phone) {
        return jdbcTemplate.query(
                selectRegistrationSql() + " where r.activity_id = ? and r.phone = ? and r.deleted = 0",
                this::mapRegistration,
                activityId,
                phone
        ).stream().findFirst();
    }

    Optional<CheckInRegistrationRecord> findRegistrationById(Long registrationId) {
        return jdbcTemplate.query(
                selectRegistrationSql() + " where r.id = ? and r.deleted = 0",
                this::mapRegistration,
                registrationId
        ).stream().findFirst();
    }

    Optional<CheckInRecord> findCheckIn(Long checkInId) {
        return jdbcTemplate.query(
                selectCheckInSql() + " where c.id = ? and c.deleted = 0",
                this::mapCheckIn,
                checkInId
        ).stream().findFirst();
    }

    Optional<CheckInRecord> findCheckInByRegistrationId(Long registrationId) {
        return jdbcTemplate.query(
                selectCheckInSql() + " where c.registration_id = ? and c.deleted = 0",
                this::mapCheckIn,
                registrationId
        ).stream().findFirst();
    }

    void insertCheckIn(Long id, Long activityId, Long registrationId, LocalDateTime checkInTime, String method, boolean manual, Long handledBy, LocalDateTime now) {
        jdbcTemplate.update(
                """
                        insert into check_in
                          (id, activity_id, registration_id, check_in_time, method, status, manual, handled_by, created_at, updated_at, deleted)
                        values (?, ?, ?, ?, ?, 'CHECKED_IN', ?, ?, ?, ?, 0)
                        """,
                id,
                activityId,
                registrationId,
                checkInTime,
                method,
                manual ? 1 : 0,
                handledBy,
                now,
                now
        );
    }

    void reactivateCheckIn(Long checkInId, LocalDateTime checkInTime, String method, boolean manual, Long handledBy, LocalDateTime now) {
        jdbcTemplate.update(
                """
                        update check_in
                        set check_in_time = ?,
                            method = ?,
                            status = 'CHECKED_IN',
                            manual = ?,
                            handled_by = ?,
                            revoked_by = null,
                            revoked_at = null,
                            updated_at = ?
                        where id = ? and deleted = 0
                        """,
                checkInTime,
                method,
                manual ? 1 : 0,
                handledBy,
                now,
                checkInId
        );
    }

    void revoke(Long checkInId, Long adminUserId, LocalDateTime now) {
        jdbcTemplate.update(
                """
                        update check_in
                        set status = 'REVOKED', revoked_by = ?, revoked_at = ?, updated_at = ?
                        where id = ? and deleted = 0
                        """,
                adminUserId,
                now,
                now,
                checkInId
        );
    }

    List<CheckInRecord> listCheckIns(Long activityId, String keyword, String status, LocalDateTime checkedFrom, LocalDateTime checkedTo, int page, int pageSize) {
        QueryParts query = filteredQuery(activityId, keyword, status, checkedFrom, checkedTo);
        query.sql.append(" order by c.check_in_time desc, c.id desc limit ? offset ?");
        query.params.add(pageSize);
        query.params.add((page - 1) * pageSize);
        return jdbcTemplate.query(selectCheckInSql() + query.sql, this::mapCheckIn, query.params.toArray());
    }

    long countCheckIns(Long activityId, String keyword, String status, LocalDateTime checkedFrom, LocalDateTime checkedTo) {
        QueryParts query = filteredQuery(activityId, keyword, status, checkedFrom, checkedTo);
        Long count = jdbcTemplate.queryForObject("select count(*) from check_in c join registration r on r.id = c.registration_id" + query.sql, Long.class, query.params.toArray());
        return count == null ? 0 : count;
    }

    private QueryParts filteredQuery(Long activityId, String keyword, String status, LocalDateTime checkedFrom, LocalDateTime checkedTo) {
        QueryParts query = new QueryParts();
        query.sql.append(" where c.deleted = 0 and r.deleted = 0 and c.activity_id = ?");
        query.params.add(activityId);
        if (StringUtils.hasText(keyword)) {
            query.sql.append(" and (lower(r.name) like ? or lower(r.phone) like ?)");
            String pattern = "%" + keyword.toLowerCase() + "%";
            query.params.add(pattern);
            query.params.add(pattern);
        }
        if (StringUtils.hasText(status)) {
            query.sql.append(" and c.status = ?");
            query.params.add(status);
        }
        if (checkedFrom != null) {
            query.sql.append(" and c.check_in_time >= ?");
            query.params.add(checkedFrom);
        }
        if (checkedTo != null) {
            query.sql.append(" and c.check_in_time <= ?");
            query.params.add(checkedTo);
        }
        return query;
    }

    private String selectRegistrationSql() {
        return """
                select r.id,
                       r.activity_id,
                       a.title as activity_title,
                       r.name,
                       r.phone,
                       r.status
                from registration r
                join activity a on a.id = r.activity_id and a.deleted = 0
                """;
    }

    private String selectCheckInSql() {
        return """
                select c.id,
                       c.activity_id,
                       a.title as activity_title,
                       c.registration_id,
                       r.name,
                       r.phone,
                       c.check_in_time,
                       c.method,
                       c.manual,
                       c.status
                from check_in c
                join activity a on a.id = c.activity_id and a.deleted = 0
                join registration r on r.id = c.registration_id
                """;
    }

    private CheckInRegistrationRecord mapRegistration(ResultSet resultSet, int rowNumber) throws SQLException {
        return new CheckInRegistrationRecord(
                resultSet.getLong("id"),
                resultSet.getLong("activity_id"),
                resultSet.getString("activity_title"),
                resultSet.getString("name"),
                resultSet.getString("phone"),
                resultSet.getString("status")
        );
    }

    private CheckInRecord mapCheckIn(ResultSet resultSet, int rowNumber) throws SQLException {
        return new CheckInRecord(
                resultSet.getLong("id"),
                resultSet.getLong("activity_id"),
                resultSet.getString("activity_title"),
                resultSet.getLong("registration_id"),
                resultSet.getString("name"),
                resultSet.getString("phone"),
                resultSet.getObject("check_in_time", LocalDateTime.class),
                resultSet.getString("method"),
                resultSet.getBoolean("manual"),
                resultSet.getString("status")
        );
    }

    private static final class QueryParts {
        private final StringBuilder sql = new StringBuilder();
        private final List<Object> params = new ArrayList<>();
    }
}
