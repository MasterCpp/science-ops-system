package com.example.scienceops.admin.activity;

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
class ActivityRepository {

    private static final String SELECT_ACTIVITY = """
            select a.id,
                   a.title,
                   a.cover_file_id,
                   a.description,
                   a.start_time,
                   a.end_time,
                   a.location,
                   a.capacity,
                   a.registration_deadline,
                   a.owner_name,
                   a.contact_phone,
                   a.plan_content,
                   a.status,
                   a.created_at,
                   a.updated_at,
                   coalesce(r.registered_attendee_count, 0) as registered_attendee_count,
                   coalesce(c.checked_in_count, 0) as checked_in_count,
                   case
                       when coalesce(r.registered_attendee_count, 0) = 0 then 0
                       else cast(coalesce(c.checked_in_count, 0) as decimal(18, 4)) / coalesce(r.registered_attendee_count, 0)
                   end as check_in_rate,
                   coalesce(va.volunteer_application_count, 0) as volunteer_application_count,
                   coalesce(va.approved_volunteer_count, 0) as approved_volunteer_count,
                   coalesce(vt.total_service_minutes, 0) as total_service_minutes,
                   coalesce(sr.survey_response_count, 0) as survey_response_count,
                   sr.average_rating as average_rating,
                   coalesce(f.photo_count, 0) as photo_count
            from activity a
            left join (
                select activity_id, sum(attendee_count) as registered_attendee_count
                from registration
                where deleted = 0 and status <> 'CANCELLED'
                group by activity_id
            ) r on r.activity_id = a.id
            left join (
                select activity_id, count(*) as checked_in_count
                from check_in
                where deleted = 0 and status = 'CHECKED_IN'
                group by activity_id
            ) c on c.activity_id = a.id
            left join (
                select activity_id,
                       count(*) as volunteer_application_count,
                       sum(case when status = 'APPROVED' then 1 else 0 end) as approved_volunteer_count
                from volunteer_application
                where deleted = 0 and status <> 'CANCELLED'
                group by activity_id
            ) va on va.activity_id = a.id
            left join (
                select activity_id,
                       sum(case
                           when status = 'REVOKED' then 0
                           when adjusted_service_minutes is not null then adjusted_service_minutes
                           when service_minutes is not null then service_minutes
                           else 0
                       end) as total_service_minutes
                from volunteer_attendance
                where deleted = 0
                group by activity_id
            ) vt on vt.activity_id = a.id
            left join (
                select s.activity_id,
                       count(distinct sr.id) as survey_response_count,
                       avg(case when sq.type = 'RATING' then sa.numeric_value else null end) as average_rating
                from survey s
                left join survey_response sr on sr.survey_id = s.id and sr.deleted = 0
                left join survey_answer sa on sa.response_id = sr.id and sa.deleted = 0
                left join survey_question sq on sq.id = sa.question_id and sq.deleted = 0
                where s.deleted = 0
                group by s.activity_id
            ) sr on sr.activity_id = a.id
            left join (
                select activity_id, count(*) as photo_count
                from file_asset
                where deleted = 0 and category = 'PHOTO'
                group by activity_id
            ) f on f.activity_id = a.id
            """;

    private final JdbcTemplate jdbcTemplate;

    ActivityRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    ActivityRecord insert(Long id, ActivityRequest request, Long adminUserId, LocalDateTime now) {
        jdbcTemplate.update(
                """
                        insert into activity (
                          id, title, cover_file_id, description, start_time, end_time, location,
                          capacity, registration_deadline, owner_name, contact_phone, plan_content,
                          status, created_by, updated_by, created_at, updated_at, deleted
                        ) values (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, 'DRAFT', ?, ?, ?, ?, 0)
                        """,
                id,
                request.title(),
                request.coverFileId(),
                request.description(),
                request.startTime(),
                request.endTime(),
                request.location(),
                request.capacity(),
                request.registrationDeadline(),
                request.ownerName(),
                request.contactPhone(),
                request.planContent(),
                adminUserId,
                adminUserId,
                now,
                now
        );
        return findById(id).orElseThrow();
    }

    void update(Long id, ActivityRequest request, Long adminUserId, LocalDateTime now) {
        jdbcTemplate.update(
                """
                        update activity
                        set title = ?,
                            cover_file_id = ?,
                            description = ?,
                            start_time = ?,
                            end_time = ?,
                            location = ?,
                            capacity = ?,
                            registration_deadline = ?,
                            owner_name = ?,
                            contact_phone = ?,
                            plan_content = ?,
                            updated_by = ?,
                            updated_at = ?
                        where id = ? and deleted = 0
                        """,
                request.title(),
                request.coverFileId(),
                request.description(),
                request.startTime(),
                request.endTime(),
                request.location(),
                request.capacity(),
                request.registrationDeadline(),
                request.ownerName(),
                request.contactPhone(),
                request.planContent(),
                adminUserId,
                now,
                id
        );
    }

    void updateStatus(Long id, String status, Long adminUserId, LocalDateTime now) {
        jdbcTemplate.update(
                "update activity set status = ?, updated_by = ?, updated_at = ? where id = ? and deleted = 0",
                status,
                adminUserId,
                now,
                id
        );
    }

    void delete(Long id, Long adminUserId, LocalDateTime now) {
        jdbcTemplate.update(
                "update activity set deleted = 1, updated_by = ?, updated_at = ? where id = ? and deleted = 0",
                adminUserId,
                now,
                id
        );
    }

    Optional<ActivityRecord> findById(Long id) {
        List<ActivityRecord> records = jdbcTemplate.query(
                SELECT_ACTIVITY + " where a.deleted = 0 and a.id = ?",
                this::mapRecord,
                id
        );
        return records.stream().findFirst();
    }

    List<ActivityRecord> list(String keyword, String status, LocalDateTime startFrom, LocalDateTime startTo, int page, int pageSize) {
        QueryParts query = filteredQuery(keyword, status, startFrom, startTo);
        query.sql.append(" order by a.start_time desc, a.id desc limit ? offset ?");
        query.params.add(pageSize);
        query.params.add((page - 1) * pageSize);
        return jdbcTemplate.query(SELECT_ACTIVITY + query.sql, this::mapRecord, query.params.toArray());
    }

    long count(String keyword, String status, LocalDateTime startFrom, LocalDateTime startTo) {
        QueryParts query = filteredQuery(keyword, status, startFrom, startTo);
        String sql = "select count(*) from activity a" + query.sql;
        Long count = jdbcTemplate.queryForObject(sql, Long.class, query.params.toArray());
        return count == null ? 0 : count;
    }

    private QueryParts filteredQuery(String keyword, String status, LocalDateTime startFrom, LocalDateTime startTo) {
        QueryParts query = new QueryParts();
        query.sql.append(" where a.deleted = 0");
        if (StringUtils.hasText(keyword)) {
            query.sql.append(" and (lower(a.title) like ? or lower(a.location) like ? or lower(a.owner_name) like ?)");
            String pattern = "%" + keyword.toLowerCase() + "%";
            query.params.add(pattern);
            query.params.add(pattern);
            query.params.add(pattern);
        }
        if (StringUtils.hasText(status)) {
            query.sql.append(" and a.status = ?");
            query.params.add(status);
        }
        if (startFrom != null) {
            query.sql.append(" and a.start_time >= ?");
            query.params.add(startFrom);
        }
        if (startTo != null) {
            query.sql.append(" and a.start_time <= ?");
            query.params.add(startTo);
        }
        return query;
    }

    private ActivityRecord mapRecord(ResultSet resultSet, int rowNumber) throws SQLException {
        return new ActivityRecord(
                resultSet.getLong("id"),
                resultSet.getString("title"),
                getNullableLong(resultSet, "cover_file_id"),
                resultSet.getString("description"),
                resultSet.getObject("start_time", LocalDateTime.class),
                resultSet.getObject("end_time", LocalDateTime.class),
                resultSet.getString("location"),
                getNullableInteger(resultSet, "capacity"),
                resultSet.getObject("registration_deadline", LocalDateTime.class),
                resultSet.getString("owner_name"),
                resultSet.getString("contact_phone"),
                resultSet.getString("plan_content"),
                resultSet.getString("status"),
                resultSet.getObject("created_at", LocalDateTime.class),
                resultSet.getObject("updated_at", LocalDateTime.class),
                resultSet.getLong("registered_attendee_count"),
                resultSet.getLong("checked_in_count"),
                resultSet.getBigDecimal("check_in_rate"),
                resultSet.getLong("volunteer_application_count"),
                resultSet.getLong("approved_volunteer_count"),
                resultSet.getLong("total_service_minutes"),
                resultSet.getLong("survey_response_count"),
                resultSet.getBigDecimal("average_rating"),
                resultSet.getLong("photo_count")
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
