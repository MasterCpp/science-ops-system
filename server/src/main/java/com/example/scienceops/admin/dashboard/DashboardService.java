package com.example.scienceops.admin.dashboard;

import java.time.Clock;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

import com.example.scienceops.security.AdminPrincipal;
import org.springframework.stereotype.Service;

@Service
public class DashboardService {

    private final DashboardRepository repository;
    private final Clock clock;

    public DashboardService(DashboardRepository repository) {
        this.repository = repository;
        this.clock = Clock.systemDefaultZone();
    }

    public DashboardSummaryResponse summary(AdminPrincipal principal) {
        return new DashboardSummaryResponse(
                can(principal, "activity:manage") ? repository.activityCountByStatus() : Map.of(),
                can(principal, "registration:manage") ? repository.registrationCount() : 0,
                can(principal, "check-in:manage") ? repository.checkInCount() : 0,
                can(principal, "volunteer:manage") ? repository.volunteerApplicationCount() : 0,
                can(principal, "volunteer:manage") ? repository.approvedVolunteerCount() : 0,
                can(principal, "volunteer:manage") ? repository.totalServiceMinutes() : 0,
                can(principal, "survey:manage") ? repository.surveyResponseCount() : 0,
                can(principal, "file:manage") ? repository.photoCount() : 0
        );
    }

    public List<DashboardUpcomingActivityResponse> upcomingActivities(AdminPrincipal principal) {
        if (!can(principal, "activity:manage")) {
            return List.of();
        }
        return repository.upcomingActivities(LocalDateTime.now(clock), 10);
    }

    public List<DashboardPendingVolunteerResponse> pendingVolunteerApplications(AdminPrincipal principal) {
        if (!can(principal, "volunteer:manage")) {
            return List.of();
        }
        return repository.pendingVolunteerApplications(10);
    }

    private boolean can(AdminPrincipal principal, String permission) {
        return principal.permissions().contains(permission);
    }
}
