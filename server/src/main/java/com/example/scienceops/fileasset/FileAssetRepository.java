package com.example.scienceops.fileasset;

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
class FileAssetRepository {

    private final JdbcTemplate jdbcTemplate;

    FileAssetRepository(JdbcTemplate jdbcTemplate) {
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

    void insert(Long id, Long activityId, String category, String originalName, String storedName,
                String mimeType, String extension, long sizeBytes, String storagePath, Long uploadedBy,
                LocalDateTime now) {
        jdbcTemplate.update(
                """
                        insert into file_asset
                          (id, activity_id, category, original_name, stored_name, mime_type, extension,
                           size_bytes, storage_path, uploaded_by, created_at, updated_at, deleted)
                        values (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, 0)
                        """,
                id,
                activityId,
                category,
                originalName,
                storedName,
                mimeType,
                extension,
                sizeBytes,
                storagePath,
                uploadedBy,
                now,
                now
        );
    }

    Optional<FileAssetRecord> findById(Long fileId) {
        return jdbcTemplate.query(
                selectSql() + " where id = ? and deleted = 0",
                this::mapRecord,
                fileId
        ).stream().findFirst();
    }

    List<FileAssetRecord> list(Long activityId, String category, String keyword, int page, int pageSize) {
        QueryParts query = filteredQuery(activityId, category, keyword);
        query.sql.append(" order by created_at desc, id desc limit ? offset ?");
        query.params.add(pageSize);
        query.params.add((page - 1) * pageSize);
        return jdbcTemplate.query(selectSql() + query.sql, this::mapRecord, query.params.toArray());
    }

    long count(Long activityId, String category, String keyword) {
        QueryParts query = filteredQuery(activityId, category, keyword);
        Long count = jdbcTemplate.queryForObject(
                "select count(*) from file_asset" + query.sql,
                Long.class,
                query.params.toArray()
        );
        return count == null ? 0 : count;
    }

    List<FileAssetRecord> listPhotosForZip(Long activityId) {
        return jdbcTemplate.query(
                selectSql() + " where deleted = 0 and activity_id = ? and category = 'PHOTO' order by created_at asc, id asc",
                this::mapRecord,
                activityId
        );
    }

    void delete(Long fileId, Long adminUserId, LocalDateTime now) {
        jdbcTemplate.update(
                "update file_asset set deleted = 1, updated_at = ? where id = ? and deleted = 0",
                now,
                fileId
        );
    }

    private QueryParts filteredQuery(Long activityId, String category, String keyword) {
        QueryParts query = new QueryParts();
        query.sql.append(" where deleted = 0 and activity_id = ?");
        query.params.add(activityId);
        if (StringUtils.hasText(category)) {
            query.sql.append(" and category = ?");
            query.params.add(category);
        }
        if (StringUtils.hasText(keyword)) {
            query.sql.append(" and lower(original_name) like ?");
            query.params.add("%" + keyword.toLowerCase() + "%");
        }
        return query;
    }

    private String selectSql() {
        return """
                select id,
                       activity_id,
                       category,
                       original_name,
                       stored_name,
                       mime_type,
                       extension,
                       size_bytes,
                       storage_path,
                       uploaded_by,
                       created_at,
                       updated_at
                from file_asset
                """;
    }

    private FileAssetRecord mapRecord(ResultSet resultSet, int rowNumber) throws SQLException {
        return new FileAssetRecord(
                resultSet.getLong("id"),
                getNullableLong(resultSet, "activity_id"),
                resultSet.getString("category"),
                resultSet.getString("original_name"),
                resultSet.getString("stored_name"),
                resultSet.getString("mime_type"),
                resultSet.getString("extension"),
                resultSet.getLong("size_bytes"),
                resultSet.getString("storage_path"),
                getNullableLong(resultSet, "uploaded_by"),
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
