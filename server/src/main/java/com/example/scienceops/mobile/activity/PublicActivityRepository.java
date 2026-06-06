package com.example.scienceops.mobile.activity;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import com.example.scienceops.admin.activity.CustomFieldResponse;
import com.example.scienceops.admin.activity.ProcessItemResponse;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
class PublicActivityRepository {

    private final JdbcTemplate jdbcTemplate;

    PublicActivityRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    Optional<PublicActivityRecord> findActivity(Long activityId) {
        return jdbcTemplate.query(
                """
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

    List<ProcessItemResponse> listProcessItems(Long activityId) {
        return jdbcTemplate.query(
                """
                        select id, activity_id, time_label, title, description, sort_order
                        from activity_process_item
                        where activity_id = ? and deleted = 0
                        order by sort_order asc, id asc
                        """,
                (resultSet, rowNumber) -> new ProcessItemResponse(
                        String.valueOf(resultSet.getLong("id")),
                        String.valueOf(resultSet.getLong("activity_id")),
                        resultSet.getString("time_label"),
                        resultSet.getString("title"),
                        resultSet.getString("description"),
                        resultSet.getInt("sort_order")
                ),
                activityId
        );
    }

    List<CustomFieldResponse> listCustomFields(Long activityId) {
        return jdbcTemplate.query(
                """
                        select id, activity_id, field_key, label, field_type, required, options_json, sort_order
                        from activity_custom_field
                        where activity_id = ? and deleted = 0
                        order by sort_order asc, id asc
                        """,
                (resultSet, rowNumber) -> new CustomFieldResponse(
                        String.valueOf(resultSet.getLong("id")),
                        String.valueOf(resultSet.getLong("activity_id")),
                        resultSet.getString("field_key"),
                        resultSet.getString("label"),
                        resultSet.getString("field_type"),
                        resultSet.getInt("required") == 1,
                        PublicJsonOptions.parse(resultSet.getString("options_json")),
                        resultSet.getInt("sort_order")
                ),
                activityId
        );
    }

    private PublicActivityRecord mapActivity(ResultSet resultSet, int rowNumber) throws SQLException {
        return new PublicActivityRecord(
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
                resultSet.getString("status"),
                resultSet.getLong("registered_attendee_count")
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
}
