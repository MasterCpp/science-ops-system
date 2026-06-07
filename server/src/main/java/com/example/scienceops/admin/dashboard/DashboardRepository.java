package com.example.scienceops.admin.dashboard;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowCallbackHandler;
import org.springframework.stereotype.Repository;

@Repository
class DashboardRepository {

    private final JdbcTemplate jdbcTemplate;

    DashboardRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    Map<String, Long> activityCountByStatus() {
        Map<String, Long> counts = new LinkedHashMap<>();
        jdbcTemplate.query(
                """
                        select status, count(*) as count
                        from activity
                        where deleted = 0
                        group by status
                        order by status asc
                        """,
                (RowCallbackHandler) resultSet -> counts.put(resultSet.getString("status"), resultSet.getLong("count"))
        );
        return counts;
    }

    long registrationCount() {
        return longValue("""
                select coalesce(sum(attendee_count), 0)
                from registration
                where deleted = 0 and status <> 'CANCELLED'
                """);
    }

    long checkInCount() {
        return longValue("""
                select count(*)
                from check_in
                where deleted = 0 and status = 'CHECKED_IN'
                """);
    }

    long volunteerApplicationCount() {
        return longValue("""
                select count(*)
                from volunteer_application
                where deleted = 0 and status <> 'CANCELLED'
                """);
    }

    long approvedVolunteerCount() {
        return longValue("""
                select count(*)
                from volunteer_application
                where deleted = 0 and status = 'APPROVED'
                """);
    }

    long totalServiceMinutes() {
        return longValue("""
                select coalesce(sum(case
                    when status = 'REVOKED' then 0
                    when adjusted_service_minutes is not null then adjusted_service_minutes
                    when service_minutes is not null then service_minutes
                    else 0
                end), 0)
                from volunteer_attendance
                where deleted = 0
                """);
    }

    long surveyResponseCount() {
        return longValue("""
                select count(*)
                from survey_response
                where deleted = 0
                """);
    }

    long photoCount() {
        return longValue("""
                select count(*)
                from file_asset
                where deleted = 0 and category = 'PHOTO'
                """);
    }

    List<DashboardUpcomingActivityResponse> upcomingActivities(LocalDateTime now, int limit) {
        return jdbcTemplate.query(
                """
                        select id, title, start_time, end_time, location, status
                        from activity
                        where deleted = 0 and start_time >= ?
                        order by start_time asc, id asc
                        limit ?
                        """,
                this::mapUpcoming,
                now,
                limit
        );
    }

    List<DashboardPendingVolunteerResponse> pendingVolunteerApplications(int limit) {
        return jdbcTemplate.query(
                """
                        select a.id as activity_id,
                               a.title as activity_title,
                               vp.id as position_id,
                               vp.name as position_name,
                               count(*) as pending_count
                        from volunteer_application va
                        join activity a on a.id = va.activity_id and a.deleted = 0
                        join volunteer_position vp on vp.id = va.position_id and vp.deleted = 0
                        where va.deleted = 0 and va.status = 'PENDING'
                        group by a.id, a.title, vp.id, vp.name
                        order by pending_count desc, a.start_time asc, vp.id asc
                        limit ?
                        """,
                this::mapPendingVolunteer,
                limit
        );
    }

    private long longValue(String sql) {
        Long value = jdbcTemplate.queryForObject(sql, Long.class);
        return value == null ? 0 : value;
    }

    private DashboardUpcomingActivityResponse mapUpcoming(ResultSet resultSet, int rowNumber) throws SQLException {
        return new DashboardUpcomingActivityResponse(
                String.valueOf(resultSet.getLong("id")),
                resultSet.getString("title"),
                resultSet.getObject("start_time", LocalDateTime.class),
                resultSet.getObject("end_time", LocalDateTime.class),
                resultSet.getString("location"),
                resultSet.getString("status")
        );
    }

    private DashboardPendingVolunteerResponse mapPendingVolunteer(ResultSet resultSet, int rowNumber) throws SQLException {
        return new DashboardPendingVolunteerResponse(
                String.valueOf(resultSet.getLong("activity_id")),
                resultSet.getString("activity_title"),
                String.valueOf(resultSet.getLong("position_id")),
                resultSet.getString("position_name"),
                resultSet.getLong("pending_count")
        );
    }
}
