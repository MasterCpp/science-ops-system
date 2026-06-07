package com.example.scienceops.operationlog;

import java.time.LocalDateTime;

public record OperationLogRecord(
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
}
