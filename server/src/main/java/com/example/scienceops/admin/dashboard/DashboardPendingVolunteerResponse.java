package com.example.scienceops.admin.dashboard;

public record DashboardPendingVolunteerResponse(
        String activityId,
        String activityTitle,
        String positionId,
        String positionName,
        long pendingCount
) {
}
