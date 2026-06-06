package com.example.scienceops.visitorreport;

import java.time.LocalDateTime;

public record VisitorReportResponse(
        String id,
        String activityId,
        String activityTitle,
        String visitorUnit,
        String contactName,
        String contactPhone,
        Integer visitorCount,
        LocalDateTime visitDate,
        String visitReason,
        String remark,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
}
