package com.example.scienceops.admin.dashboard;

import java.util.Map;

public record DashboardSummaryResponse(
        Map<String, Long> activityCountByStatus,
        long registrationCount,
        long checkInCount,
        long volunteerApplicationCount,
        long approvedVolunteerCount,
        long totalServiceMinutes,
        long surveyResponseCount,
        long photoCount
) {
}
