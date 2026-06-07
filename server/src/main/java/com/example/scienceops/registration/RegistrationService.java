package com.example.scienceops.registration;

import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

import com.baomidou.mybatisplus.core.toolkit.IdWorker;
import com.example.scienceops.common.api.PagedResponse;
import com.example.scienceops.common.enums.ActivityStatus;
import com.example.scienceops.common.enums.RegistrationStatus;
import com.example.scienceops.common.error.BusinessRuleException;
import com.example.scienceops.common.error.NotFoundException;
import com.example.scienceops.operationlog.OperationLogService;
import com.example.scienceops.security.AdminPrincipal;
import org.springframework.stereotype.Service;

@Service
public class RegistrationService {

    private final RegistrationRepository repository;
    private final OperationLogService operationLogService;
    private final Clock clock;

    public RegistrationService(RegistrationRepository repository, OperationLogService operationLogService) {
        this.repository = repository;
        this.operationLogService = operationLogService;
        this.clock = Clock.systemDefaultZone();
    }

    public RegistrationResponse submitPublic(Long activityId, RegistrationRequest request) {
        RegistrationActivityRecord activity = requireActivity(activityId);
        if (ActivityStatus.valueOf(activity.status()) != ActivityStatus.REGISTRATION_OPEN) {
            throw new BusinessRuleException("INVALID_STATE", "Activity is not open for registration", 409);
        }
        if (activity.registrationDeadline() != null && LocalDateTime.now(clock).isAfter(activity.registrationDeadline())) {
            throw new BusinessRuleException("DEADLINE_PASSED", "Registration deadline has passed", 409);
        }
        return createRegistration(activity, request);
    }

    public RegistrationResponse backfill(Long activityId, RegistrationRequest request, AdminPrincipal principal) {
        RegistrationActivityRecord activity = requireActivity(activityId);
        if (ActivityStatus.valueOf(activity.status()) == ActivityStatus.ARCHIVED) {
            throw new BusinessRuleException("INVALID_STATE", "Archived activity cannot accept backfilled registrations", 409);
        }
        RegistrationResponse response = createRegistration(activity, request);
        operationLogService.record(principal, "REGISTRATION_BACKFILL", "REGISTRATION", Long.valueOf(response.id()), response.name(), Map.of(
                "activityId", activity.id(),
                "phone", response.phone()
        ));
        return response;
    }

    public PagedResponse<RegistrationResponse> list(Long activityId, String keyword, String status, int page, int pageSize) {
        requireActivity(activityId);
        int safePage = Math.max(page, 1);
        int safePageSize = Math.max(1, Math.min(pageSize, 100));
        return new PagedResponse<>(
                repository.listRegistrations(activityId, keyword, status, safePage, safePageSize)
                        .stream()
                        .map(this::toResponse)
                        .toList(),
                safePage,
                safePageSize,
                repository.countRegistrations(activityId, keyword, status)
        );
    }

    public RegistrationResponse cancel(Long registrationId, AdminPrincipal principal) {
        RegistrationRecord registration = repository.findRegistration(registrationId)
                .orElseThrow(() -> new NotFoundException("Registration not found"));
        repository.cancel(registrationId, principal.id(), LocalDateTime.now(clock));
        RegistrationResponse response = repository.findRegistration(registrationId)
                .map(this::toResponse)
                .orElseThrow(() -> new NotFoundException("Registration not found"));
        operationLogService.record(principal, "REGISTRATION_CANCEL", "REGISTRATION", registration.id(), registration.name(), Map.of(
                "activityId", registration.activityId(),
                "phone", registration.phone()
        ));
        return response;
    }

    public byte[] exportCsv(Long activityId) {
        requireActivity(activityId);
        List<RegistrationResponse> registrations = repository.listRegistrations(activityId, null, null, 1, 10000)
                .stream()
                .map(this::toResponse)
                .toList();
        StringBuilder csv = new StringBuilder("\uFEFF");
        csv.append("ID,Activity,Name,Phone,Attendee Count,Unit/School,Age Group,Status,Remark,Custom Values,Created At\n");
        for (RegistrationResponse registration : registrations) {
            csv.append(csvCell(registration.id())).append(',')
                    .append(csvCell(registration.activityTitle())).append(',')
                    .append(csvCell(registration.name())).append(',')
                    .append(csvCell(registration.phone())).append(',')
                    .append(registration.attendeeCount()).append(',')
                    .append(csvCell(registration.unitName())).append(',')
                    .append(csvCell(registration.ageGroup())).append(',')
                    .append(csvCell(registration.status())).append(',')
                    .append(csvCell(registration.remark())).append(',')
                    .append(csvCell(customValuesText(registration.customValues()))).append(',')
                    .append(csvCell(String.valueOf(registration.createdAt()))).append('\n');
        }
        return csv.toString().getBytes(StandardCharsets.UTF_8);
    }

    private RegistrationResponse createRegistration(RegistrationActivityRecord activity, RegistrationRequest request) {
        if (repository.activePhoneExists(activity.id(), request.phone())) {
            throw new BusinessRuleException("DUPLICATE_SUBMISSION", "Phone already registered for this activity", 409);
        }
        if (activity.capacity() != null && activity.registeredAttendeeCount() + request.attendeeCount() > activity.capacity()) {
            throw new BusinessRuleException("CAPACITY_FULL", "Activity capacity is full", 409);
        }

        LocalDateTime now = LocalDateTime.now(clock);
        Long registrationId = IdWorker.getId();
        repository.insertRegistration(registrationId, activity.id(), request, RegistrationStatus.REGISTERED.name(), now);
        insertCustomValues(activity.id(), registrationId, request, now);
        return repository.findRegistration(registrationId)
                .map(this::toResponse)
                .orElseThrow(() -> new NotFoundException("Registration not found"));
    }

    private void insertCustomValues(Long activityId, Long registrationId, RegistrationRequest request, LocalDateTime now) {
        Map<String, RegistrationRepository.CustomFieldDefinition> definitions = repository.listCustomFieldDefinitions(activityId)
                .stream()
                .collect(Collectors.toMap(RegistrationRepository.CustomFieldDefinition::fieldKey, Function.identity()));
        List<RegistrationCustomValueRequest> values = request.customValues() == null ? List.of() : request.customValues();
        for (RegistrationCustomValueRequest value : values) {
            RegistrationRepository.CustomFieldDefinition definition = definitions.get(value.fieldKey());
            if (definition != null) {
                repository.insertCustomValue(IdWorker.getId(), registrationId, definition, value.value(), now);
            }
        }
    }

    private RegistrationActivityRecord requireActivity(Long activityId) {
        return repository.findActivity(activityId)
                .orElseThrow(() -> new NotFoundException("Activity not found"));
    }

    private RegistrationResponse toResponse(RegistrationRecord registration) {
        return new RegistrationResponse(
                String.valueOf(registration.id()),
                String.valueOf(registration.activityId()),
                registration.activityTitle(),
                registration.name(),
                registration.phone(),
                registration.attendeeCount(),
                registration.unitName(),
                registration.ageGroup(),
                registration.remark(),
                registration.status(),
                repository.listCustomValues(registration.id()),
                registration.createdAt(),
                registration.updatedAt()
        );
    }

    private String customValuesText(List<RegistrationCustomValueResponse> customValues) {
        return customValues.stream()
                .map(value -> value.label() + "=" + value.value())
                .collect(Collectors.joining("; "));
    }

    private String csvCell(String value) {
        String safeValue = value == null ? "" : value;
        return "\"" + safeValue.replace("\"", "\"\"") + "\"";
    }
}
