package com.example.scienceops.operationlog;

import java.time.LocalDateTime;

public record OperationLogResponse(
        String id,
        String adminUserId,
        String adminUsername,
        String adminRoleCode,
        String action,
        String targetType,
        String targetId,
        String targetSummary,
        String ip,
        String userAgent,
        String detailJson,
        LocalDateTime createdAt
) {
}
