package com.example.scienceops.admin.dashboard;

import java.time.LocalDateTime;

public record DashboardUpcomingActivityResponse(
        String id,
        String title,
        LocalDateTime startTime,
        LocalDateTime endTime,
        String location,
        String status
) {
}
