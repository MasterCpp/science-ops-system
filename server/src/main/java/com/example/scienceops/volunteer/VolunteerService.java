package com.example.scienceops.volunteer;

import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;

import com.baomidou.mybatisplus.core.toolkit.IdWorker;
import com.example.scienceops.common.api.PagedResponse;
import com.example.scienceops.common.enums.ActivityStatus;
import com.example.scienceops.common.enums.VolunteerApplicationStatus;
import com.example.scienceops.common.enums.VolunteerAttendanceStatus;
import com.example.scienceops.common.error.BusinessRuleException;
import com.example.scienceops.common.error.NotFoundException;
import com.example.scienceops.security.AdminPrincipal;
import org.springframework.stereotype.Service;

@Service
public class VolunteerService {

    private final VolunteerRepository repository;
    private final Clock clock;

    public VolunteerService(VolunteerRepository repository) {
        this.repository = repository;
        this.clock = Clock.systemDefaultZone();
    }

    public VolunteerPositionResponse createPosition(Long activityId, VolunteerPositionRequest request, AdminPrincipal principal) {
        requireActivity(activityId);
        validateServiceTime(request);
        LocalDateTime now = LocalDateTime.now(clock);
        Long positionId = IdWorker.getId();
        repository.insertPosition(positionId, activityId, request, principal.id(), now);
        return repository.findPosition(positionId)
                .map(this::toPositionResponse)
                .orElseThrow(() -> new NotFoundException("Volunteer position not found"));
    }

    public VolunteerPositionResponse updatePosition(Long positionId, VolunteerPositionRequest request, AdminPrincipal principal) {
        repository.findPosition(positionId)
                .orElseThrow(() -> new NotFoundException("Volunteer position not found"));
        validateServiceTime(request);
        repository.updatePosition(positionId, request, principal.id(), LocalDateTime.now(clock));
        return repository.findPosition(positionId)
                .map(this::toPositionResponse)
                .orElseThrow(() -> new NotFoundException("Volunteer position not found"));
    }

    public VolunteerPositionResponse deletePosition(Long positionId, AdminPrincipal principal) {
        VolunteerPositionRecord position = repository.findPosition(positionId)
                .orElseThrow(() -> new NotFoundException("Volunteer position not found"));
        repository.deletePosition(positionId, principal.id(), LocalDateTime.now(clock));
        return toPositionResponse(position);
    }

    public List<VolunteerPositionResponse> listPositions(Long activityId) {
        requireActivity(activityId);
        return repository.listPositions(activityId).stream()
                .map(this::toPositionResponse)
                .toList();
    }

    public VolunteerApplicationResponse submitPublic(Long activityId, VolunteerApplicationRequest request) {
        requireActivity(activityId);
        Long positionId = parseId(request.positionId(), "Position id is invalid");
        VolunteerPositionRecord position = repository.findPosition(positionId)
                .orElseThrow(() -> new NotFoundException("Volunteer position not found"));
        if (!position.activityId().equals(activityId)) {
            throw new NotFoundException("Volunteer position not found");
        }
        if (repository.applicationPhoneExists(activityId, request.phone())) {
            throw new BusinessRuleException("DUPLICATE_SUBMISSION", "Phone already applied for this activity", 409);
        }
        if (position.approvedCount() >= position.capacity()) {
            throw new BusinessRuleException("CAPACITY_FULL", "Volunteer position is full", 409);
        }

        LocalDateTime now = LocalDateTime.now(clock);
        Long applicationId = IdWorker.getId();
        repository.insertApplication(applicationId, activityId, positionId, request, VolunteerApplicationStatus.PENDING.name(), now);
        return repository.findApplication(applicationId)
                .map(this::toApplicationResponse)
                .orElseThrow(() -> new NotFoundException("Volunteer application not found"));
    }

    public PagedResponse<VolunteerApplicationResponse> listApplications(Long activityId, Long positionId, String keyword, String status, int page, int pageSize) {
        if (activityId != null) {
            requireActivity(activityId);
        }
        int safePage = Math.max(page, 1);
        int safePageSize = Math.max(1, Math.min(pageSize, 100));
        return new PagedResponse<>(
                repository.listApplications(activityId, positionId, keyword, status, safePage, safePageSize)
                        .stream()
                        .map(this::toApplicationResponse)
                        .toList(),
                safePage,
                safePageSize,
                repository.countApplications(activityId, positionId, keyword, status)
        );
    }

    public VolunteerApplicationResponse approve(Long applicationId, VolunteerApplicationReviewRequest request, AdminPrincipal principal) {
        VolunteerApplicationRecord application = requireApplication(applicationId);
        VolunteerPositionRecord position = repository.findPosition(application.positionId())
                .orElseThrow(() -> new NotFoundException("Volunteer position not found"));
        if (VolunteerApplicationStatus.valueOf(application.status()) != VolunteerApplicationStatus.APPROVED
                && position.approvedCount() >= position.capacity()) {
            throw new BusinessRuleException("CAPACITY_FULL", "Volunteer position is full", 409);
        }
        return review(applicationId, VolunteerApplicationStatus.APPROVED, request, principal);
    }

    public VolunteerApplicationResponse reject(Long applicationId, VolunteerApplicationReviewRequest request, AdminPrincipal principal) {
        requireApplication(applicationId);
        return review(applicationId, VolunteerApplicationStatus.REJECTED, request, principal);
    }

    public VolunteerApplicationResponse cancel(Long applicationId, VolunteerApplicationReviewRequest request, AdminPrincipal principal) {
        requireApplication(applicationId);
        return review(applicationId, VolunteerApplicationStatus.CANCELLED, request, principal);
    }

    public byte[] exportCsv(Long activityId, Long positionId, String status) {
        if (activityId != null) {
            requireActivity(activityId);
        }
        List<VolunteerApplicationResponse> applications = repository.listApplications(activityId, positionId, null, status, 1, 10000)
                .stream()
                .map(this::toApplicationResponse)
                .toList();
        StringBuilder csv = new StringBuilder("\uFEFF");
        csv.append("ID,Activity,Position,Name,Phone,Unit/School,Age Group,Status,Available Time,Experience,Remark,Review Note,Reviewed At\n");
        for (VolunteerApplicationResponse application : applications) {
            csv.append(csvCell(application.id())).append(',')
                    .append(csvCell(application.activityTitle())).append(',')
                    .append(csvCell(application.positionName())).append(',')
                    .append(csvCell(application.name())).append(',')
                    .append(csvCell(application.phone())).append(',')
                    .append(csvCell(application.unitName())).append(',')
                    .append(csvCell(application.ageGroup())).append(',')
                    .append(csvCell(application.status())).append(',')
                    .append(csvCell(application.availableTimeNote())).append(',')
                    .append(csvCell(application.experienceNote())).append(',')
                    .append(csvCell(application.remark())).append(',')
                    .append(csvCell(application.reviewNote())).append(',')
                    .append(csvCell(String.valueOf(application.reviewedAt()))).append('\n');
        }
        return csv.toString().getBytes(StandardCharsets.UTF_8);
    }

    public VolunteerAttendanceStatusResponse lookupAttendanceStatus(Long activityId, String phone) {
        requireActivity(activityId);
        VolunteerApplicationRecord application = repository.findApplicationByPhone(activityId, phone)
                .orElseThrow(() -> new NotFoundException("Volunteer application not found"));
        return new VolunteerAttendanceStatusResponse(
                toApplicationResponse(application),
                repository.findAttendanceByApplicationId(application.id()).map(this::toAttendanceResponse).orElse(null)
        );
    }

    public VolunteerAttendanceResponse publicCheckIn(Long applicationId) {
        VolunteerApplicationRecord application = requireApplication(applicationId);
        requireApproved(application);
        return createOrReactivateAttendance(application, LocalDateTime.now(clock), null);
    }

    public VolunteerAttendanceResponse publicCheckOut(Long applicationId) {
        VolunteerApplicationRecord application = requireApplication(applicationId);
        requireApproved(application);
        VolunteerAttendanceRecord attendance = repository.findAttendanceByApplicationId(applicationId)
                .orElseThrow(() -> new BusinessRuleException("NOT_CHECKED_IN", "Volunteer has not checked in", 409));
        return checkOut(attendance, LocalDateTime.now(clock), null);
    }

    public VolunteerAttendanceResponse manualCheckIn(Long activityId, ManualVolunteerCheckInRequest request, AdminPrincipal principal) {
        requireActivity(activityId);
        Long applicationId = parseId(request.applicationId(), "Application id is invalid");
        VolunteerApplicationRecord application = requireApplication(applicationId);
        if (!application.activityId().equals(activityId)) {
            throw new NotFoundException("Volunteer application not found");
        }
        requireApproved(application);
        LocalDateTime checkInTime = request.checkInTime() == null ? LocalDateTime.now(clock) : request.checkInTime();
        return createOrReactivateAttendance(application, checkInTime, principal.id());
    }

    public VolunteerAttendanceResponse manualCheckOut(Long applicationId, ManualVolunteerCheckOutRequest request, AdminPrincipal principal) {
        VolunteerApplicationRecord application = requireApplication(applicationId);
        requireApproved(application);
        VolunteerAttendanceRecord attendance = repository.findAttendanceByApplicationId(applicationId)
                .orElseThrow(() -> new BusinessRuleException("NOT_CHECKED_IN", "Volunteer has not checked in", 409));
        LocalDateTime checkOutTime = request == null || request.checkOutTime() == null ? LocalDateTime.now(clock) : request.checkOutTime();
        return checkOut(attendance, checkOutTime, principal.id());
    }

    public VolunteerAttendanceResponse adjustAttendance(Long attendanceId, VolunteerAttendanceAdjustRequest request, AdminPrincipal principal) {
        VolunteerAttendanceRecord attendance = repository.findAttendance(attendanceId)
                .orElseThrow(() -> new NotFoundException("Volunteer attendance not found"));
        if (VolunteerAttendanceStatus.valueOf(attendance.status()) == VolunteerAttendanceStatus.REVOKED) {
            throw new BusinessRuleException("INVALID_STATE", "Revoked attendance cannot be adjusted", 409);
        }
        repository.adjustAttendance(attendanceId, request.serviceMinutes(), request.adjustmentReason(), principal.id(), LocalDateTime.now(clock));
        return repository.findAttendance(attendanceId)
                .map(this::toAttendanceResponse)
                .orElseThrow(() -> new NotFoundException("Volunteer attendance not found"));
    }

    public VolunteerAttendanceResponse revokeAttendance(Long attendanceId, AdminPrincipal principal) {
        repository.findAttendance(attendanceId)
                .orElseThrow(() -> new NotFoundException("Volunteer attendance not found"));
        repository.revokeAttendance(attendanceId, principal.id(), LocalDateTime.now(clock));
        return repository.findAttendance(attendanceId)
                .map(this::toAttendanceResponse)
                .orElseThrow(() -> new NotFoundException("Volunteer attendance not found"));
    }

    public PagedResponse<VolunteerAttendanceResponse> listAttendances(Long activityId, Long positionId, String keyword, String status, int page, int pageSize) {
        if (activityId != null) {
            requireActivity(activityId);
        }
        int safePage = Math.max(page, 1);
        int safePageSize = Math.max(1, Math.min(pageSize, 100));
        return new PagedResponse<>(
                repository.listAttendances(activityId, positionId, keyword, status, safePage, safePageSize)
                        .stream()
                        .map(this::toAttendanceResponse)
                        .toList(),
                safePage,
                safePageSize,
                repository.countAttendances(activityId, positionId, keyword, status)
        );
    }

    private VolunteerApplicationResponse review(Long applicationId, VolunteerApplicationStatus status, VolunteerApplicationReviewRequest request, AdminPrincipal principal) {
        repository.reviewApplication(applicationId, status.name(), principal.id(), request == null ? null : request.reviewNote(), LocalDateTime.now(clock));
        return repository.findApplication(applicationId)
                .map(this::toApplicationResponse)
                .orElseThrow(() -> new NotFoundException("Volunteer application not found"));
    }

    private VolunteerAttendanceResponse createOrReactivateAttendance(VolunteerApplicationRecord application, LocalDateTime checkInTime, Long handledBy) {
        LocalDateTime now = LocalDateTime.now(clock);
        VolunteerAttendanceRecord existing = repository.findAttendanceByApplicationId(application.id()).orElse(null);
        if (existing != null && VolunteerAttendanceStatus.valueOf(existing.status()) != VolunteerAttendanceStatus.REVOKED) {
            throw new BusinessRuleException("ALREADY_CHECKED_IN", "Volunteer has already checked in", 409);
        }
        if (existing != null) {
            repository.reactivateAttendance(existing.id(), checkInTime, handledBy, now);
            return repository.findAttendance(existing.id())
                    .map(this::toAttendanceResponse)
                    .orElseThrow(() -> new NotFoundException("Volunteer attendance not found"));
        }

        Long attendanceId = IdWorker.getId();
        repository.insertAttendance(attendanceId, application.activityId(), application.id(), checkInTime, handledBy, now);
        return repository.findAttendance(attendanceId)
                .map(this::toAttendanceResponse)
                .orElseThrow(() -> new NotFoundException("Volunteer attendance not found"));
    }

    private VolunteerAttendanceResponse checkOut(VolunteerAttendanceRecord attendance, LocalDateTime checkOutTime, Long handledBy) {
        VolunteerAttendanceStatus status = VolunteerAttendanceStatus.valueOf(attendance.status());
        if (status == VolunteerAttendanceStatus.CHECKED_OUT) {
            throw new BusinessRuleException("ALREADY_CHECKED_OUT", "Volunteer has already checked out", 409);
        }
        if (status == VolunteerAttendanceStatus.REVOKED) {
            throw new BusinessRuleException("INVALID_STATE", "Volunteer attendance is revoked", 409);
        }
        if (checkOutTime.isBefore(attendance.checkInTime())) {
            throw new BusinessRuleException("INVALID_REQUEST", "Check-out time cannot be before check-in time", 400);
        }
        int serviceMinutes = Math.toIntExact(ChronoUnit.MINUTES.between(attendance.checkInTime(), checkOutTime));
        repository.checkOutAttendance(attendance.id(), checkOutTime, serviceMinutes, handledBy, LocalDateTime.now(clock));
        return repository.findAttendance(attendance.id())
                .map(this::toAttendanceResponse)
                .orElseThrow(() -> new NotFoundException("Volunteer attendance not found"));
    }

    private VolunteerActivityRecord requireActivity(Long activityId) {
        return repository.findActivity(activityId)
                .orElseThrow(() -> new NotFoundException("Activity not found"));
    }

    private VolunteerApplicationRecord requireApplication(Long applicationId) {
        return repository.findApplication(applicationId)
                .orElseThrow(() -> new NotFoundException("Volunteer application not found"));
    }

    private void requireApproved(VolunteerApplicationRecord application) {
        if (VolunteerApplicationStatus.valueOf(application.status()) != VolunteerApplicationStatus.APPROVED) {
            throw new BusinessRuleException("NOT_APPROVED", "Volunteer application is not approved", 409);
        }
    }

    private void validateServiceTime(VolunteerPositionRequest request) {
        if (!request.serviceEndTime().isAfter(request.serviceStartTime())) {
            throw new BusinessRuleException("INVALID_REQUEST", "Service end time must be after start time", 400);
        }
    }

    private Long parseId(String value, String message) {
        try {
            return Long.valueOf(value);
        } catch (NumberFormatException exception) {
            throw new BusinessRuleException("INVALID_REQUEST", message, 400);
        }
    }

    private VolunteerPositionResponse toPositionResponse(VolunteerPositionRecord position) {
        int remaining = Math.max(0, position.capacity() - Math.toIntExact(position.approvedCount()));
        return new VolunteerPositionResponse(
                String.valueOf(position.id()),
                String.valueOf(position.activityId()),
                position.activityTitle(),
                position.name(),
                position.description(),
                position.capacity(),
                position.approvedCount(),
                remaining,
                remaining <= 0,
                position.serviceStartTime(),
                position.serviceEndTime(),
                position.createdAt(),
                position.updatedAt()
        );
    }

    private VolunteerApplicationResponse toApplicationResponse(VolunteerApplicationRecord application) {
        return new VolunteerApplicationResponse(
                String.valueOf(application.id()),
                String.valueOf(application.activityId()),
                application.activityTitle(),
                String.valueOf(application.positionId()),
                application.positionName(),
                application.name(),
                application.phone(),
                application.unitName(),
                application.ageGroup(),
                application.availableTimeNote(),
                application.experienceNote(),
                application.remark(),
                application.status(),
                application.reviewedBy() == null ? null : String.valueOf(application.reviewedBy()),
                application.reviewedAt(),
                application.reviewNote(),
                application.createdAt(),
                application.updatedAt()
        );
    }

    private VolunteerAttendanceResponse toAttendanceResponse(VolunteerAttendanceRecord attendance) {
        Integer effectiveMinutes = effectiveServiceMinutes(attendance);
        return new VolunteerAttendanceResponse(
                String.valueOf(attendance.id()),
                String.valueOf(attendance.activityId()),
                attendance.activityTitle(),
                String.valueOf(attendance.applicationId()),
                String.valueOf(attendance.positionId()),
                attendance.positionName(),
                attendance.name(),
                attendance.phone(),
                attendance.checkInTime(),
                attendance.checkOutTime(),
                attendance.serviceMinutes(),
                effectiveMinutes,
                attendance.status(),
                attendance.manuallyAdjusted(),
                attendance.adjustedServiceMinutes(),
                attendance.adjustmentReason(),
                attendance.createdAt(),
                attendance.updatedAt()
        );
    }

    private Integer effectiveServiceMinutes(VolunteerAttendanceRecord attendance) {
        if (VolunteerAttendanceStatus.valueOf(attendance.status()) == VolunteerAttendanceStatus.REVOKED) {
            return 0;
        }
        if (attendance.manuallyAdjusted()) {
            return attendance.adjustedServiceMinutes();
        }
        return attendance.serviceMinutes();
    }

    private String csvCell(String value) {
        String safeValue = value == null ? "" : value;
        return "\"" + safeValue.replace("\"", "\"\"") + "\"";
    }
}
