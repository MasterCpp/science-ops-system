package com.example.scienceops.admin.dashboard;

import java.util.List;

import com.example.scienceops.common.api.ApiResponse;
import com.example.scienceops.security.AdminPrincipal;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/admin/dashboard")
@PreAuthorize("isAuthenticated()")
public class AdminDashboardController {

    private final DashboardService service;

    public AdminDashboardController(DashboardService service) {
        this.service = service;
    }

    @GetMapping("/summary")
    public ApiResponse<DashboardSummaryResponse> summary(@AuthenticationPrincipal AdminPrincipal principal) {
        return ApiResponse.ok(service.summary(principal));
    }

    @GetMapping("/upcoming-activities")
    public ApiResponse<List<DashboardUpcomingActivityResponse>> upcomingActivities(@AuthenticationPrincipal AdminPrincipal principal) {
        return ApiResponse.ok(service.upcomingActivities(principal));
    }

    @GetMapping("/pending-volunteer-applications")
    public ApiResponse<List<DashboardPendingVolunteerResponse>> pendingVolunteerApplications(@AuthenticationPrincipal AdminPrincipal principal) {
        return ApiResponse.ok(service.pendingVolunteerApplications(principal));
    }
}
