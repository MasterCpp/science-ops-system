package com.example.scienceops.admin.activity;

import java.time.Clock;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.Objects;

import com.baomidou.mybatisplus.core.toolkit.IdWorker;
import com.example.scienceops.common.api.PagedResponse;
import com.example.scienceops.common.enums.ActivityStatus;
import com.example.scienceops.common.error.ForbiddenException;
import com.example.scienceops.common.error.InvalidStateException;
import com.example.scienceops.common.error.NotFoundException;
import com.example.scienceops.operationlog.OperationLogService;
import com.example.scienceops.security.AdminPrincipal;
import org.springframework.stereotype.Service;

@Service
class ActivityService {

    private final ActivityRepository repository;
    private final OperationLogService operationLogService;
    private final Clock clock;

    ActivityService(ActivityRepository repository, OperationLogService operationLogService) {
        this.repository = repository;
        this.operationLogService = operationLogService;
        this.clock = Clock.systemDefaultZone();
    }

    ActivityResponse create(ActivityRequest request, AdminPrincipal principal) {
        validateTimeRange(request);
        ActivityRecord activity = repository.insert(IdWorker.getId(), request, principal.id(), now());
        operationLogService.record(principal, "ACTIVITY_CREATE", "ACTIVITY", activity.id(), activity.title(), Map.of(
                "status", activity.status()
        ));
        return activity.toResponse();
    }

    PagedResponse<ActivityResponse> list(String keyword, String status, LocalDateTime startFrom, LocalDateTime startTo, int page, int pageSize) {
        int safePage = Math.max(page, 1);
        int safePageSize = Math.max(1, Math.min(pageSize, 100));
        if (status != null && !status.isBlank()) {
            parseStatus(status);
        }
        return new PagedResponse<>(
                repository.list(keyword, status, startFrom, startTo, safePage, safePageSize)
                        .stream()
                        .map(ActivityRecord::toResponse)
                        .toList(),
                safePage,
                safePageSize,
                repository.count(keyword, status, startFrom, startTo)
        );
    }

    ActivityResponse detail(Long activityId) {
        return load(activityId).toResponse();
    }

    ActivityResponse update(Long activityId, ActivityRequest request, AdminPrincipal principal) {
        validateTimeRange(request);
        ActivityRecord current = load(activityId);
        ActivityStatus status = parseStatus(current.status());
        if (status == ActivityStatus.ARCHIVED) {
            throw new InvalidStateException("Archived activities are read-only");
        }
        if (status == ActivityStatus.IN_PROGRESS && registrationSensitiveFieldsChanged(current, request)) {
            throw new InvalidStateException("In-progress activities cannot change registration deadline or capacity");
        }
        repository.update(activityId, request, principal.id(), now());
        ActivityRecord updated = load(activityId);
        operationLogService.record(principal, "ACTIVITY_UPDATE", "ACTIVITY", updated.id(), updated.title(), Map.of(
                "status", updated.status()
        ));
        return updated.toResponse();
    }

    ActivityResponse publish(Long activityId, AdminPrincipal principal) {
        return transition(activityId, ActivityStatus.DRAFT, ActivityStatus.REGISTRATION_OPEN, principal);
    }

    ActivityResponse start(Long activityId, AdminPrincipal principal) {
        return transition(activityId, ActivityStatus.REGISTRATION_OPEN, ActivityStatus.IN_PROGRESS, principal);
    }

    ActivityResponse end(Long activityId, AdminPrincipal principal) {
        return transition(activityId, ActivityStatus.IN_PROGRESS, ActivityStatus.ENDED, principal);
    }

    ActivityResponse archive(Long activityId, AdminPrincipal principal) {
        return transition(activityId, ActivityStatus.ENDED, ActivityStatus.ARCHIVED, principal);
    }

    ActivityResponse unarchive(Long activityId, AdminPrincipal principal) {
        requireSuperAdmin(principal);
        return transition(activityId, ActivityStatus.ARCHIVED, ActivityStatus.ENDED, principal, "ACTIVITY_UNARCHIVE");
    }

    void delete(Long activityId, AdminPrincipal principal) {
        requireSuperAdmin(principal);
        ActivityRecord current = load(activityId);
        repository.delete(activityId, principal.id(), now());
        operationLogService.record(principal, "ACTIVITY_DELETE", "ACTIVITY", current.id(), current.title(), Map.of(
                "status", current.status()
        ));
    }

    private ActivityResponse transition(Long activityId, ActivityStatus from, ActivityStatus to, AdminPrincipal principal) {
        String action = switch (to) {
            case REGISTRATION_OPEN -> "ACTIVITY_PUBLISH";
            case IN_PROGRESS -> "ACTIVITY_START";
            case ENDED -> "ACTIVITY_END";
            case ARCHIVED -> "ACTIVITY_ARCHIVE";
            case DRAFT -> "ACTIVITY_UPDATE";
        };
        return transition(activityId, from, to, principal, action);
    }

    private ActivityResponse transition(Long activityId, ActivityStatus from, ActivityStatus to, AdminPrincipal principal, String action) {
        ActivityRecord current = load(activityId);
        ActivityStatus currentStatus = parseStatus(current.status());
        if (currentStatus != from) {
            throw new InvalidStateException("Activity status must be " + from + " before it can become " + to);
        }
        repository.updateStatus(activityId, to.name(), principal.id(), now());
        ActivityRecord updated = load(activityId);
        operationLogService.record(principal, action, "ACTIVITY", updated.id(), updated.title(), Map.of(
                "fromStatus", from.name(),
                "toStatus", to.name()
        ));
        return updated.toResponse();
    }

    private ActivityRecord load(Long activityId) {
        return repository.findById(activityId)
                .orElseThrow(() -> new NotFoundException("Activity not found"));
    }

    private ActivityStatus parseStatus(String status) {
        try {
            return ActivityStatus.valueOf(status);
        } catch (IllegalArgumentException exception) {
            throw new InvalidStateException("Unknown activity status: " + status);
        }
    }

    private void validateTimeRange(ActivityRequest request) {
        if (!request.endTime().isAfter(request.startTime())) {
            throw new InvalidStateException("End time must be after start time");
        }
    }

    private boolean registrationSensitiveFieldsChanged(ActivityRecord current, ActivityRequest request) {
        return !Objects.equals(current.capacity(), request.capacity())
                || !Objects.equals(current.registrationDeadline(), request.registrationDeadline());
    }

    private void requireSuperAdmin(AdminPrincipal principal) {
        if (!principal.roles().contains("SUPER_ADMIN")) {
            throw new ForbiddenException("Only super admin can perform this operation");
        }
    }

    private LocalDateTime now() {
        return LocalDateTime.now(clock);
    }
}
