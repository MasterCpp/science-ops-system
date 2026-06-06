package com.example.scienceops.registration;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;
import org.springframework.util.StringUtils;

@Repository
public class RegistrationRepository {

    private final JdbcTemplate jdbcTemplate;

    public RegistrationRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    Optional<RegistrationActivityRecord> findActivity(Long activityId) {
        return jdbcTemplate.query(
                """
                        select a.id,
                               a.title,
                               a.capacity,
                               a.registration_deadline,
                               a.status,
                               coalesce(r.registered_attendee_count, 0) as registered_attendee_count
                        from activity a
                        left join (
                            select activity_id, sum(attendee_count) as registered_attendee_count
                            from registration
                            where deleted = 0 and status <> 'CANCELLED'
                            group by activity_id
                        ) r on r.activity_id = a.id
                        where a.id = ? and a.deleted = 0
                        """,
                this::mapActivity,
                activityId
        ).stream().findFirst();
    }

    boolean activePhoneExists(Long activityId, String phone) {
        Long count = jdbcTemplate.queryForObject(
                """
                        select count(*)
                        from registration
                        where activity_id = ? and phone = ? and deleted = 0 and status <> 'CANCELLED'
                        """,
                Long.class,
                activityId,
                phone
        );
        return count != null && count > 0;
    }

    void insertRegistration(Long id, Long activityId, RegistrationRequest request, String status, LocalDateTime now) {
        jdbcTemplate.update(
                """
                        insert into registration
                          (id, activity_id, name, phone, attendee_count, unit_name, age_group, remark, status, created_at, updated_at, deleted)
                        values (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, 0)
                        """,
                id,
                activityId,
                request.name(),
                request.phone(),
                request.attendeeCount(),
                request.unitName(),
                request.ageGroup(),
                request.remark(),
                status,
                now,
                now
        );
    }

    List<CustomFieldDefinition> listCustomFieldDefinitions(Long activityId) {
        return jdbcTemplate.query(
                """
                        select id, field_key, label
                        from activity_custom_field
                        where activity_id = ? and deleted = 0
                        order by sort_order asc, id asc
                        """,
                (resultSet, rowNumber) -> new CustomFieldDefinition(
                        resultSet.getLong("id"),
                        resultSet.getString("field_key"),
                        resultSet.getString("label")
                ),
                activityId
        );
    }

    void insertCustomValue(Long id, Long registrationId, CustomFieldDefinition definition, String value, LocalDateTime now) {
        jdbcTemplate.update(
                """
                        insert into registration_custom_value
                          (id, registration_id, custom_field_id, field_key, label, value_text, created_at, updated_at, deleted)
                        values (?, ?, ?, ?, ?, ?, ?, ?, 0)
                        """,
                id,
                registrationId,
                definition.id(),
                definition.fieldKey(),
                definition.label(),
                value,
                now,
                now
        );
    }

    Optional<RegistrationRecord> findRegistration(Long registrationId) {
        return jdbcTemplate.query(
                """
                        select r.id,
                               r.activity_id,
                               a.title as activity_title,
                               r.name,
                               r.phone,
                               r.attendee_count,
                               r.unit_name,
                               r.age_group,
                               r.remark,
                               r.status,
                               r.created_at,
                               r.updated_at
                        from registration r
                        join activity a on a.id = r.activity_id
                        where r.id = ? and r.deleted = 0
                        """,
                this::mapRegistration,
                registrationId
        ).stream().findFirst();
    }

    List<RegistrationRecord> listRegistrations(Long activityId, String keyword, String status, int page, int pageSize) {
        QueryParts query = filteredQuery(activityId, keyword, status);
        query.sql.append(" order by r.created_at desc, r.id desc limit ? offset ?");
        query.params.add(pageSize);
        query.params.add((page - 1) * pageSize);
        return jdbcTemplate.query(selectRegistrationSql() + query.sql, this::mapRegistration, query.params.toArray());
    }

    long countRegistrations(Long activityId, String keyword, String status) {
        QueryParts query = filteredQuery(activityId, keyword, status);
        Long count = jdbcTemplate.queryForObject("select count(*) from registration r" + query.sql, Long.class, query.params.toArray());
        return count == null ? 0 : count;
    }

    List<RegistrationCustomValueResponse> listCustomValues(Long registrationId) {
        return jdbcTemplate.query(
                """
                        select field_key, label, value_text
                        from registration_custom_value
                        where registration_id = ? and deleted = 0
                        order by id asc
                        """,
                (resultSet, rowNumber) -> new RegistrationCustomValueResponse(
                        resultSet.getString("field_key"),
                        resultSet.getString("label"),
                        resultSet.getString("value_text")
                ),
                registrationId
        );
    }

    void cancel(Long registrationId, Long adminUserId, LocalDateTime now) {
        jdbcTemplate.update(
                """
                        update registration
                        set status = 'CANCELLED', cancelled_by = ?, cancelled_at = ?, updated_at = ?
                        where id = ? and deleted = 0 and status <> 'CANCELLED'
                        """,
                adminUserId,
                now,
                now,
                registrationId
        );
    }

    private QueryParts filteredQuery(Long activityId, String keyword, String status) {
        QueryParts query = new QueryParts();
        query.sql.append(" where r.deleted = 0 and r.activity_id = ?");
        query.params.add(activityId);
        if (StringUtils.hasText(keyword)) {
            query.sql.append(" and (lower(r.name) like ? or lower(r.phone) like ? or lower(r.unit_name) like ?)");
            String pattern = "%" + keyword.toLowerCase() + "%";
            query.params.add(pattern);
            query.params.add(pattern);
            query.params.add(pattern);
        }
        if (StringUtils.hasText(status)) {
            query.sql.append(" and r.status = ?");
            query.params.add(status);
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
                       r.attendee_count,
                       r.unit_name,
                       r.age_group,
                       r.remark,
                       r.status,
                       r.created_at,
                       r.updated_at
                from registration r
                join activity a on a.id = r.activity_id
                """;
    }

    private RegistrationActivityRecord mapActivity(ResultSet resultSet, int rowNumber) throws SQLException {
        return new RegistrationActivityRecord(
                resultSet.getLong("id"),
                resultSet.getString("title"),
                getNullableInteger(resultSet, "capacity"),
                resultSet.getObject("registration_deadline", LocalDateTime.class),
                resultSet.getString("status"),
                resultSet.getLong("registered_attendee_count")
        );
    }

    private RegistrationRecord mapRegistration(ResultSet resultSet, int rowNumber) throws SQLException {
        return new RegistrationRecord(
                resultSet.getLong("id"),
                resultSet.getLong("activity_id"),
                resultSet.getString("activity_title"),
                resultSet.getString("name"),
                resultSet.getString("phone"),
                resultSet.getInt("attendee_count"),
                resultSet.getString("unit_name"),
                resultSet.getString("age_group"),
                resultSet.getString("remark"),
                resultSet.getString("status"),
                resultSet.getObject("created_at", LocalDateTime.class),
                resultSet.getObject("updated_at", LocalDateTime.class)
        );
    }

    private Integer getNullableInteger(ResultSet resultSet, String column) throws SQLException {
        int value = resultSet.getInt(column);
        return resultSet.wasNull() ? null : value;
    }

    public record CustomFieldDefinition(Long id, String fieldKey, String label) {
    }

    private static final class QueryParts {
        private final StringBuilder sql = new StringBuilder();
        private final List<Object> params = new java.util.ArrayList<>();
    }
}
