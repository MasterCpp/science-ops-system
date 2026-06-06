package com.example.scienceops.volunteer;

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
class VolunteerRepository {

    private final JdbcTemplate jdbcTemplate;

    VolunteerRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    Optional<VolunteerActivityRecord> findActivity(Long activityId) {
        return jdbcTemplate.query(
                """
                        select id, title, status
                        from activity
                        where id = ? and deleted = 0
                        """,
                (resultSet, rowNumber) -> new VolunteerActivityRecord(
                        resultSet.getLong("id"),
                        resultSet.getString("title"),
                        resultSet.getString("status")
                ),
                activityId
        ).stream().findFirst();
    }

    Optional<VolunteerPositionRecord> findPosition(Long positionId) {
        return jdbcTemplate.query(
                selectPositionSql() + " where p.id = ? and p.deleted = 0",
                this::mapPosition,
                positionId
        ).stream().findFirst();
    }

    List<VolunteerPositionRecord> listPositions(Long activityId) {
        return jdbcTemplate.query(
                selectPositionSql() + " where p.activity_id = ? and p.deleted = 0 order by p.service_start_time asc, p.id asc",
                this::mapPosition,
                activityId
        );
    }

    void insertPosition(Long id, Long activityId, VolunteerPositionRequest request, Long adminUserId, LocalDateTime now) {
        jdbcTemplate.update(
                """
                        insert into volunteer_position
                          (id, activity_id, name, description, capacity, service_start_time, service_end_time,
                           created_by, updated_by, created_at, updated_at, deleted)
                        values (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, 0)
                        """,
                id,
                activityId,
                request.name(),
                request.description(),
                request.capacity(),
                request.serviceStartTime(),
                request.serviceEndTime(),
                adminUserId,
                adminUserId,
                now,
                now
        );
    }

    void updatePosition(Long positionId, VolunteerPositionRequest request, Long adminUserId, LocalDateTime now) {
        jdbcTemplate.update(
                """
                        update volunteer_position
                        set name = ?,
                            description = ?,
                            capacity = ?,
                            service_start_time = ?,
                            service_end_time = ?,
                            updated_by = ?,
                            updated_at = ?
                        where id = ? and deleted = 0
                        """,
                request.name(),
                request.description(),
                request.capacity(),
                request.serviceStartTime(),
                request.serviceEndTime(),
                adminUserId,
                now,
                positionId
        );
    }

    void deletePosition(Long positionId, Long adminUserId, LocalDateTime now) {
        jdbcTemplate.update(
                """
                        update volunteer_position
                        set deleted = 1, updated_by = ?, updated_at = ?
                        where id = ? and deleted = 0
                        """,
                adminUserId,
                now,
                positionId
        );
    }

    boolean applicationPhoneExists(Long activityId, String phone) {
        Long count = jdbcTemplate.queryForObject(
                """
                        select count(*)
                        from volunteer_application
                        where activity_id = ? and phone = ? and deleted = 0
                        """,
                Long.class,
                activityId,
                phone
        );
        return count != null && count > 0;
    }

    void insertApplication(Long id, Long activityId, Long positionId, VolunteerApplicationRequest request, String status, LocalDateTime now) {
        jdbcTemplate.update(
                """
                        insert into volunteer_application
                          (id, activity_id, position_id, name, phone, unit_name, age_group,
                           available_time_note, experience_note, remark, status, created_at, updated_at, deleted)
                        values (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, 0)
                        """,
                id,
                activityId,
                positionId,
                request.name(),
                request.phone(),
                request.unitName(),
                request.ageGroup(),
                request.availableTimeNote(),
                request.experienceNote(),
                request.remark(),
                status,
                now,
                now
        );
    }

    Optional<VolunteerApplicationRecord> findApplication(Long applicationId) {
        return jdbcTemplate.query(
                selectApplicationSql() + " where va.id = ? and va.deleted = 0",
                this::mapApplication,
                applicationId
        ).stream().findFirst();
    }

    Optional<VolunteerApplicationRecord> findApplicationByPhone(Long activityId, String phone) {
        return jdbcTemplate.query(
                selectApplicationSql() + " where va.activity_id = ? and va.phone = ? and va.deleted = 0",
                this::mapApplication,
                activityId,
                phone
        ).stream().findFirst();
    }

    void reviewApplication(Long applicationId, String status, Long adminUserId, String reviewNote, LocalDateTime now) {
        jdbcTemplate.update(
                """
                        update volunteer_application
                        set status = ?, reviewed_by = ?, reviewed_at = ?, review_note = ?, updated_at = ?
                        where id = ? and deleted = 0
                        """,
                status,
                adminUserId,
                now,
                reviewNote,
                now,
                applicationId
        );
    }

    List<VolunteerApplicationRecord> listApplications(Long activityId, Long positionId, String keyword, String status, int page, int pageSize) {
        QueryParts query = filteredApplicationQuery(activityId, positionId, keyword, status);
        query.sql.append(" order by va.created_at desc, va.id desc limit ? offset ?");
        query.params.add(pageSize);
        query.params.add((page - 1) * pageSize);
        return jdbcTemplate.query(selectApplicationSql() + query.sql, this::mapApplication, query.params.toArray());
    }

    long countApplications(Long activityId, Long positionId, String keyword, String status) {
        QueryParts query = filteredApplicationQuery(activityId, positionId, keyword, status);
        Long count = jdbcTemplate.queryForObject(
                "select count(*) from volunteer_application va join volunteer_position p on p.id = va.position_id join activity a on a.id = va.activity_id" + query.sql,
                Long.class,
                query.params.toArray()
        );
        return count == null ? 0 : count;
    }

    Optional<VolunteerAttendanceRecord> findAttendance(Long attendanceId) {
        return jdbcTemplate.query(
                selectAttendanceSql() + " where att.id = ? and att.deleted = 0",
                this::mapAttendance,
                attendanceId
        ).stream().findFirst();
    }

    Optional<VolunteerAttendanceRecord> findAttendanceByApplicationId(Long applicationId) {
        return jdbcTemplate.query(
                selectAttendanceSql() + " where att.application_id = ? and att.deleted = 0",
                this::mapAttendance,
                applicationId
        ).stream().findFirst();
    }

    void insertAttendance(Long id, Long activityId, Long applicationId, LocalDateTime checkInTime, Long handledBy, LocalDateTime now) {
        jdbcTemplate.update(
                """
                        insert into volunteer_attendance
                          (id, activity_id, application_id, check_in_time, service_minutes, status,
                           manually_adjusted, handled_by, created_at, updated_at, deleted)
                        values (?, ?, ?, ?, 0, 'CHECKED_IN', 0, ?, ?, ?, 0)
                        """,
                id,
                activityId,
                applicationId,
                checkInTime,
                handledBy,
                now,
                now
        );
    }

    void reactivateAttendance(Long attendanceId, LocalDateTime checkInTime, Long handledBy, LocalDateTime now) {
        jdbcTemplate.update(
                """
                        update volunteer_attendance
                        set check_in_time = ?,
                            check_out_time = null,
                            service_minutes = 0,
                            status = 'CHECKED_IN',
                            manually_adjusted = 0,
                            adjusted_service_minutes = null,
                            adjustment_reason = null,
                            handled_by = ?,
                            revoked_by = null,
                            revoked_at = null,
                            updated_at = ?
                        where id = ? and deleted = 0
                        """,
                checkInTime,
                handledBy,
                now,
                attendanceId
        );
    }

    void checkOutAttendance(Long attendanceId, LocalDateTime checkOutTime, Integer serviceMinutes, Long handledBy, LocalDateTime now) {
        jdbcTemplate.update(
                """
                        update volunteer_attendance
                        set check_out_time = ?,
                            service_minutes = ?,
                            status = 'CHECKED_OUT',
                            handled_by = ?,
                            updated_at = ?
                        where id = ? and deleted = 0
                        """,
                checkOutTime,
                serviceMinutes,
                handledBy,
                now,
                attendanceId
        );
    }

    void adjustAttendance(Long attendanceId, Integer serviceMinutes, String reason, Long handledBy, LocalDateTime now) {
        jdbcTemplate.update(
                """
                        update volunteer_attendance
                        set manually_adjusted = 1,
                            adjusted_service_minutes = ?,
                            adjustment_reason = ?,
                            handled_by = ?,
                            updated_at = ?
                        where id = ? and deleted = 0
                        """,
                serviceMinutes,
                reason,
                handledBy,
                now,
                attendanceId
        );
    }

    void revokeAttendance(Long attendanceId, Long adminUserId, LocalDateTime now) {
        jdbcTemplate.update(
                """
                        update volunteer_attendance
                        set status = 'REVOKED',
                            revoked_by = ?,
                            revoked_at = ?,
                            updated_at = ?
                        where id = ? and deleted = 0
                        """,
                adminUserId,
                now,
                now,
                attendanceId
        );
    }

    List<VolunteerAttendanceRecord> listAttendances(Long activityId, Long positionId, String keyword, String status, int page, int pageSize) {
        QueryParts query = filteredAttendanceQuery(activityId, positionId, keyword, status);
        query.sql.append(" order by att.check_in_time desc, att.id desc limit ? offset ?");
        query.params.add(pageSize);
        query.params.add((page - 1) * pageSize);
        return jdbcTemplate.query(selectAttendanceSql() + query.sql, this::mapAttendance, query.params.toArray());
    }

    long countAttendances(Long activityId, Long positionId, String keyword, String status) {
        QueryParts query = filteredAttendanceQuery(activityId, positionId, keyword, status);
        Long count = jdbcTemplate.queryForObject(
                "select count(*) from volunteer_attendance att join volunteer_application va on va.id = att.application_id join volunteer_position p on p.id = va.position_id join activity a on a.id = att.activity_id" + query.sql,
                Long.class,
                query.params.toArray()
        );
        return count == null ? 0 : count;
    }

    private QueryParts filteredApplicationQuery(Long activityId, Long positionId, String keyword, String status) {
        QueryParts query = new QueryParts();
        query.sql.append(" where va.deleted = 0 and p.deleted = 0 and a.deleted = 0");
        if (activityId != null) {
            query.sql.append(" and va.activity_id = ?");
            query.params.add(activityId);
        }
        if (positionId != null) {
            query.sql.append(" and va.position_id = ?");
            query.params.add(positionId);
        }
        if (StringUtils.hasText(keyword)) {
            query.sql.append(" and (lower(va.name) like ? or lower(va.phone) like ? or lower(va.unit_name) like ?)");
            String pattern = "%" + keyword.toLowerCase() + "%";
            query.params.add(pattern);
            query.params.add(pattern);
            query.params.add(pattern);
        }
        if (StringUtils.hasText(status)) {
            query.sql.append(" and va.status = ?");
            query.params.add(status);
        }
        return query;
    }

    private QueryParts filteredAttendanceQuery(Long activityId, Long positionId, String keyword, String status) {
        QueryParts query = new QueryParts();
        query.sql.append(" where att.deleted = 0 and va.deleted = 0 and p.deleted = 0 and a.deleted = 0");
        if (activityId != null) {
            query.sql.append(" and att.activity_id = ?");
            query.params.add(activityId);
        }
        if (positionId != null) {
            query.sql.append(" and va.position_id = ?");
            query.params.add(positionId);
        }
        if (StringUtils.hasText(keyword)) {
            query.sql.append(" and (lower(va.name) like ? or lower(va.phone) like ? or lower(va.unit_name) like ?)");
            String pattern = "%" + keyword.toLowerCase() + "%";
            query.params.add(pattern);
            query.params.add(pattern);
            query.params.add(pattern);
        }
        if (StringUtils.hasText(status)) {
            query.sql.append(" and att.status = ?");
            query.params.add(status);
        }
        return query;
    }

    private String selectPositionSql() {
        return """
                select p.id,
                       p.activity_id,
                       a.title as activity_title,
                       p.name,
                       p.description,
                       p.capacity,
                       coalesce(approved.approved_count, 0) as approved_count,
                       p.service_start_time,
                       p.service_end_time,
                       p.created_at,
                       p.updated_at
                from volunteer_position p
                join activity a on a.id = p.activity_id and a.deleted = 0
                left join (
                    select position_id, count(*) as approved_count
                    from volunteer_application
                    where deleted = 0 and status = 'APPROVED'
                    group by position_id
                ) approved on approved.position_id = p.id
                """;
    }

    private String selectApplicationSql() {
        return """
                select va.id,
                       va.activity_id,
                       a.title as activity_title,
                       va.position_id,
                       p.name as position_name,
                       va.name,
                       va.phone,
                       va.unit_name,
                       va.age_group,
                       va.available_time_note,
                       va.experience_note,
                       va.remark,
                       va.status,
                       va.reviewed_by,
                       va.reviewed_at,
                       va.review_note,
                       va.created_at,
                       va.updated_at
                from volunteer_application va
                join activity a on a.id = va.activity_id and a.deleted = 0
                join volunteer_position p on p.id = va.position_id and p.deleted = 0
                """;
    }

    private String selectAttendanceSql() {
        return """
                select att.id,
                       att.activity_id,
                       a.title as activity_title,
                       att.application_id,
                       va.position_id,
                       p.name as position_name,
                       va.name,
                       va.phone,
                       att.check_in_time,
                       att.check_out_time,
                       att.service_minutes,
                       att.status,
                       att.manually_adjusted,
                       att.adjusted_service_minutes,
                       att.adjustment_reason,
                       att.created_at,
                       att.updated_at
                from volunteer_attendance att
                join activity a on a.id = att.activity_id and a.deleted = 0
                join volunteer_application va on va.id = att.application_id and va.deleted = 0
                join volunteer_position p on p.id = va.position_id and p.deleted = 0
                """;
    }

    private VolunteerPositionRecord mapPosition(ResultSet resultSet, int rowNumber) throws SQLException {
        return new VolunteerPositionRecord(
                resultSet.getLong("id"),
                resultSet.getLong("activity_id"),
                resultSet.getString("activity_title"),
                resultSet.getString("name"),
                resultSet.getString("description"),
                resultSet.getInt("capacity"),
                resultSet.getLong("approved_count"),
                resultSet.getObject("service_start_time", LocalDateTime.class),
                resultSet.getObject("service_end_time", LocalDateTime.class),
                resultSet.getObject("created_at", LocalDateTime.class),
                resultSet.getObject("updated_at", LocalDateTime.class)
        );
    }

    private VolunteerApplicationRecord mapApplication(ResultSet resultSet, int rowNumber) throws SQLException {
        return new VolunteerApplicationRecord(
                resultSet.getLong("id"),
                resultSet.getLong("activity_id"),
                resultSet.getString("activity_title"),
                resultSet.getLong("position_id"),
                resultSet.getString("position_name"),
                resultSet.getString("name"),
                resultSet.getString("phone"),
                resultSet.getString("unit_name"),
                resultSet.getString("age_group"),
                resultSet.getString("available_time_note"),
                resultSet.getString("experience_note"),
                resultSet.getString("remark"),
                resultSet.getString("status"),
                getNullableLong(resultSet, "reviewed_by"),
                resultSet.getObject("reviewed_at", LocalDateTime.class),
                resultSet.getString("review_note"),
                resultSet.getObject("created_at", LocalDateTime.class),
                resultSet.getObject("updated_at", LocalDateTime.class)
        );
    }

    private VolunteerAttendanceRecord mapAttendance(ResultSet resultSet, int rowNumber) throws SQLException {
        return new VolunteerAttendanceRecord(
                resultSet.getLong("id"),
                resultSet.getLong("activity_id"),
                resultSet.getString("activity_title"),
                resultSet.getLong("application_id"),
                resultSet.getLong("position_id"),
                resultSet.getString("position_name"),
                resultSet.getString("name"),
                resultSet.getString("phone"),
                resultSet.getObject("check_in_time", LocalDateTime.class),
                resultSet.getObject("check_out_time", LocalDateTime.class),
                getNullableInteger(resultSet, "service_minutes"),
                resultSet.getString("status"),
                resultSet.getBoolean("manually_adjusted"),
                getNullableInteger(resultSet, "adjusted_service_minutes"),
                resultSet.getString("adjustment_reason"),
                resultSet.getObject("created_at", LocalDateTime.class),
                resultSet.getObject("updated_at", LocalDateTime.class)
        );
    }

    private Long getNullableLong(ResultSet resultSet, String column) throws SQLException {
        long value = resultSet.getLong(column);
        return resultSet.wasNull() ? null : value;
    }

    private Integer getNullableInteger(ResultSet resultSet, String column) throws SQLException {
        int value = resultSet.getInt(column);
        return resultSet.wasNull() ? null : value;
    }

    private static final class QueryParts {
        private final StringBuilder sql = new StringBuilder();
        private final List<Object> params = new ArrayList<>();
    }
}
