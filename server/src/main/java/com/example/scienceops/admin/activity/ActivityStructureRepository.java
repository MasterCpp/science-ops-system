package com.example.scienceops.admin.activity;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.util.List;

import com.baomidou.mybatisplus.core.toolkit.IdWorker;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
class ActivityStructureRepository {

    private final JdbcTemplate jdbcTemplate;

    ActivityStructureRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    List<ProcessItemResponse> listProcessItems(Long activityId) {
        return jdbcTemplate.query(
                """
                        select id, activity_id, time_label, title, description, sort_order
                        from activity_process_item
                        where activity_id = ? and deleted = 0
                        order by sort_order asc, id asc
                        """,
                this::mapProcessItem,
                activityId
        );
    }

    ProcessItemResponse insertProcessItem(Long activityId, ProcessItemRequest request, Long adminUserId, LocalDateTime now) {
        Long id = IdWorker.getId();
        jdbcTemplate.update(
                """
                        insert into activity_process_item
                          (id, activity_id, time_label, title, description, sort_order, created_by, updated_by, created_at, updated_at, deleted)
                        values (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, 0)
                        """,
                id,
                activityId,
                request.timeLabel(),
                request.title(),
                request.description(),
                request.sortOrder(),
                adminUserId,
                adminUserId,
                now,
                now
        );
        return getProcessItem(activityId, id);
    }

    ProcessItemResponse updateProcessItem(Long activityId, Long itemId, ProcessItemRequest request, Long adminUserId, LocalDateTime now) {
        jdbcTemplate.update(
                """
                        update activity_process_item
                        set time_label = ?, title = ?, description = ?, sort_order = ?, updated_by = ?, updated_at = ?
                        where id = ? and activity_id = ? and deleted = 0
                        """,
                request.timeLabel(),
                request.title(),
                request.description(),
                request.sortOrder(),
                adminUserId,
                now,
                itemId,
                activityId
        );
        return getProcessItem(activityId, itemId);
    }

    void deleteProcessItem(Long activityId, Long itemId, Long adminUserId, LocalDateTime now) {
        jdbcTemplate.update(
                "update activity_process_item set deleted = 1, updated_by = ?, updated_at = ? where id = ? and activity_id = ? and deleted = 0",
                adminUserId,
                now,
                itemId,
                activityId
        );
    }

    ProcessItemResponse getProcessItem(Long activityId, Long itemId) {
        return jdbcTemplate.query(
                """
                        select id, activity_id, time_label, title, description, sort_order
                        from activity_process_item
                        where id = ? and activity_id = ? and deleted = 0
                        """,
                this::mapProcessItem,
                itemId,
                activityId
        ).stream().findFirst().orElseThrow();
    }

    List<CustomFieldResponse> listCustomFields(Long activityId) {
        return jdbcTemplate.query(
                """
                        select id, activity_id, field_key, label, field_type, required, options_json, sort_order
                        from activity_custom_field
                        where activity_id = ? and deleted = 0
                        order by sort_order asc, id asc
                        """,
                this::mapCustomField,
                activityId
        );
    }

    boolean customFieldKeyExists(Long activityId, String fieldKey, Long excludedFieldId) {
        Long count = jdbcTemplate.queryForObject(
                """
                        select count(*)
                        from activity_custom_field
                        where activity_id = ? and field_key = ? and deleted = 0 and (? is null or id <> ?)
                        """,
                Long.class,
                activityId,
                fieldKey,
                excludedFieldId,
                excludedFieldId
        );
        return count != null && count > 0;
    }

    CustomFieldResponse insertCustomField(Long activityId, CustomFieldRequest request, String optionsJson, Long adminUserId, LocalDateTime now) {
        Long id = IdWorker.getId();
        jdbcTemplate.update(
                """
                        insert into activity_custom_field
                          (id, activity_id, field_key, label, field_type, required, options_json, sort_order, created_by, updated_by, created_at, updated_at, deleted)
                        values (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, 0)
                        """,
                id,
                activityId,
                request.fieldKey(),
                request.label(),
                request.fieldType(),
                Boolean.TRUE.equals(request.required()) ? 1 : 0,
                optionsJson,
                request.sortOrder(),
                adminUserId,
                adminUserId,
                now,
                now
        );
        return getCustomField(activityId, id);
    }

    CustomFieldResponse updateCustomField(Long activityId, Long fieldId, CustomFieldRequest request, String optionsJson, Long adminUserId, LocalDateTime now) {
        jdbcTemplate.update(
                """
                        update activity_custom_field
                        set field_key = ?, label = ?, field_type = ?, required = ?, options_json = ?, sort_order = ?, updated_by = ?, updated_at = ?
                        where id = ? and activity_id = ? and deleted = 0
                        """,
                request.fieldKey(),
                request.label(),
                request.fieldType(),
                Boolean.TRUE.equals(request.required()) ? 1 : 0,
                optionsJson,
                request.sortOrder(),
                adminUserId,
                now,
                fieldId,
                activityId
        );
        return getCustomField(activityId, fieldId);
    }

    void deleteCustomField(Long activityId, Long fieldId, Long adminUserId, LocalDateTime now) {
        jdbcTemplate.update(
                "update activity_custom_field set deleted = 1, updated_by = ?, updated_at = ? where id = ? and activity_id = ? and deleted = 0",
                adminUserId,
                now,
                fieldId,
                activityId
        );
    }

    CustomFieldResponse getCustomField(Long activityId, Long fieldId) {
        return jdbcTemplate.query(
                """
                        select id, activity_id, field_key, label, field_type, required, options_json, sort_order
                        from activity_custom_field
                        where id = ? and activity_id = ? and deleted = 0
                        """,
                this::mapCustomField,
                fieldId,
                activityId
        ).stream().findFirst().orElseThrow();
    }

    private ProcessItemResponse mapProcessItem(ResultSet resultSet, int rowNumber) throws SQLException {
        return new ProcessItemResponse(
                String.valueOf(resultSet.getLong("id")),
                String.valueOf(resultSet.getLong("activity_id")),
                resultSet.getString("time_label"),
                resultSet.getString("title"),
                resultSet.getString("description"),
                resultSet.getInt("sort_order")
        );
    }

    private CustomFieldResponse mapCustomField(ResultSet resultSet, int rowNumber) throws SQLException {
        return new CustomFieldResponse(
                String.valueOf(resultSet.getLong("id")),
                String.valueOf(resultSet.getLong("activity_id")),
                resultSet.getString("field_key"),
                resultSet.getString("label"),
                resultSet.getString("field_type"),
                resultSet.getInt("required") == 1,
                JsonOptions.parse(resultSet.getString("options_json")),
                resultSet.getInt("sort_order")
        );
    }
}
