package com.example.scienceops.checkin;

import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

import com.baomidou.mybatisplus.core.toolkit.IdWorker;
import com.example.scienceops.common.api.PagedResponse;
import com.example.scienceops.common.enums.ActivityStatus;
import com.example.scienceops.common.enums.CheckInStatus;
import com.example.scienceops.common.enums.RegistrationStatus;
import com.example.scienceops.common.error.BusinessRuleException;
import com.example.scienceops.common.error.NotFoundException;
import com.example.scienceops.operationlog.OperationLogService;
import com.example.scienceops.security.AdminPrincipal;
import org.springframework.stereotype.Service;

@Service
public class CheckInService {

    private final CheckInRepository repository;
    private final OperationLogService operationLogService;
    private final Clock clock;

    public CheckInService(CheckInRepository repository, OperationLogService operationLogService) {
        this.repository = repository;
        this.operationLogService = operationLogService;
        this.clock = Clock.systemDefaultZone();
    }

    public CheckInResponse submitPublic(Long activityId, CheckInRequest request) {
        CheckInActivityRecord activity = requireActivity(activityId);
        if (ActivityStatus.valueOf(activity.status()) != ActivityStatus.IN_PROGRESS) {
            throw new BusinessRuleException("INVALID_STATE", "Activity is not in progress", 409);
        }
        CheckInRegistrationRecord registration = repository.findRegistrationByPhone(activityId, request.phone())
                .orElseThrow(() -> new NotFoundException("Registration not found"));
        requireRegistered(registration);
        return createOrReactivate(registration, LocalDateTime.now(clock), "QR", false, null);
    }

    public CheckInResponse manualCheckIn(Long activityId, ManualCheckInRequest request, AdminPrincipal principal) {
        requireActivity(activityId);
        Long registrationId = parseId(request.registrationId(), "Registration id is invalid");
        CheckInRegistrationRecord registration = repository.findRegistrationById(registrationId)
                .orElseThrow(() -> new NotFoundException("Registration not found"));
        if (!registration.activityId().equals(activityId)) {
            throw new NotFoundException("Registration not found");
        }
        requireRegistered(registration);
        LocalDateTime checkInTime = request.checkInTime() == null ? LocalDateTime.now(clock) : request.checkInTime();
        CheckInResponse response = createOrReactivate(registration, checkInTime, "MANUAL", true, principal.id());
        operationLogService.record(principal, "CHECK_IN_MANUAL", "CHECK_IN", Long.valueOf(response.id()), response.name(), Map.of(
                "activityId", activityId,
                "registrationId", registration.id(),
                "phone", registration.phone()
        ));
        return response;
    }

    public CheckInResponse revoke(Long checkInId, AdminPrincipal principal) {
        CheckInRecord current = repository.findCheckIn(checkInId)
                .orElseThrow(() -> new NotFoundException("Check-in not found"));
        repository.revoke(checkInId, principal.id(), LocalDateTime.now(clock));
        CheckInResponse response = repository.findCheckIn(checkInId)
                .map(this::toResponse)
                .orElseThrow(() -> new NotFoundException("Check-in not found"));
        operationLogService.record(principal, "CHECK_IN_REVOKE", "CHECK_IN", current.id(), current.name(), Map.of(
                "activityId", current.activityId(),
                "registrationId", current.registrationId(),
                "phone", current.phone()
        ));
        return response;
    }

    public PagedResponse<CheckInResponse> list(Long activityId, String keyword, String status, LocalDateTime checkedFrom, LocalDateTime checkedTo, int page, int pageSize) {
        requireActivity(activityId);
        int safePage = Math.max(page, 1);
        int safePageSize = Math.max(1, Math.min(pageSize, 100));
        return new PagedResponse<>(
                repository.listCheckIns(activityId, keyword, status, checkedFrom, checkedTo, safePage, safePageSize)
                        .stream()
                        .map(this::toResponse)
                        .toList(),
                safePage,
                safePageSize,
                repository.countCheckIns(activityId, keyword, status, checkedFrom, checkedTo)
        );
    }

    public byte[] exportCsv(Long activityId) {
        requireActivity(activityId);
        List<CheckInResponse> checkIns = repository.listCheckIns(activityId, null, null, null, null, 1, 10000)
                .stream()
                .map(this::toResponse)
                .toList();
        StringBuilder csv = new StringBuilder("\uFEFF");
        csv.append("ID,Activity,Registration ID,Name,Phone,Check-in Time,Method,Manual,Status\n");
        for (CheckInResponse checkIn : checkIns) {
            csv.append(csvCell(checkIn.id())).append(',')
                    .append(csvCell(checkIn.activityTitle())).append(',')
                    .append(csvCell(checkIn.registrationId())).append(',')
                    .append(csvCell(checkIn.name())).append(',')
                    .append(csvCell(checkIn.phone())).append(',')
                    .append(csvCell(String.valueOf(checkIn.checkInTime()))).append(',')
                    .append(csvCell(checkIn.method())).append(',')
                    .append(checkIn.manual()).append(',')
                    .append(csvCell(checkIn.status())).append('\n');
        }
        return csv.toString().getBytes(StandardCharsets.UTF_8);
    }

    private CheckInResponse createOrReactivate(CheckInRegistrationRecord registration, LocalDateTime checkInTime, String method, boolean manual, Long handledBy) {
        LocalDateTime now = LocalDateTime.now(clock);
        CheckInRecord existing = repository.findCheckInByRegistrationId(registration.id()).orElse(null);
        if (existing != null && CheckInStatus.valueOf(existing.status()) == CheckInStatus.CHECKED_IN) {
            throw new BusinessRuleException("ALREADY_CHECKED_IN", "Registration has already checked in", 409);
        }
        if (existing != null) {
            repository.reactivateCheckIn(existing.id(), checkInTime, method, manual, handledBy, now);
            return repository.findCheckIn(existing.id())
                    .map(this::toResponse)
                    .orElseThrow(() -> new NotFoundException("Check-in not found"));
        }

        Long checkInId = IdWorker.getId();
        repository.insertCheckIn(checkInId, registration.activityId(), registration.id(), checkInTime, method, manual, handledBy, now);
        return repository.findCheckIn(checkInId)
                .map(this::toResponse)
                .orElseThrow(() -> new NotFoundException("Check-in not found"));
    }

    private CheckInActivityRecord requireActivity(Long activityId) {
        return repository.findActivity(activityId)
                .orElseThrow(() -> new NotFoundException("Activity not found"));
    }

    private void requireRegistered(CheckInRegistrationRecord registration) {
        if (RegistrationStatus.valueOf(registration.status()) == RegistrationStatus.CANCELLED) {
            throw new BusinessRuleException("INVALID_STATE", "Registration is cancelled", 409);
        }
    }

    private Long parseId(String value, String message) {
        try {
            return Long.valueOf(value);
        } catch (NumberFormatException exception) {
            throw new BusinessRuleException("INVALID_REQUEST", message, 400);
        }
    }

    private CheckInResponse toResponse(CheckInRecord checkIn) {
        return new CheckInResponse(
                String.valueOf(checkIn.id()),
                String.valueOf(checkIn.activityId()),
                checkIn.activityTitle(),
                String.valueOf(checkIn.registrationId()),
                checkIn.name(),
                checkIn.phone(),
                checkIn.checkInTime(),
                checkIn.method(),
                checkIn.manual(),
                checkIn.status()
        );
    }

    private String csvCell(String value) {
        String safeValue = value == null ? "" : value;
        return "\"" + safeValue.replace("\"", "\"\"") + "\"";
    }
}
