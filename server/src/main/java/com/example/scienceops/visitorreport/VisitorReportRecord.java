package com.example.scienceops.visitorreport;

import java.time.LocalDateTime;

public record VisitorReportRecord(
        Long id,
        Long activityId,
        String activityTitle,
        String visitorUnit,
        String contactName,
        String contactPhone,
        Integer visitorCount,
        LocalDateTime visitDate,
        String visitReason,
        String remark,
        Long createdBy,
        Long updatedBy,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
}
