package com.example.scienceops.admin.activity;

import java.time.Clock;
import java.time.LocalDateTime;
import java.util.List;

import com.example.scienceops.common.enums.ActivityStatus;
import com.example.scienceops.common.error.ConflictException;
import com.example.scienceops.common.error.InvalidStateException;
import com.example.scienceops.common.error.NotFoundException;
import com.example.scienceops.security.AdminPrincipal;
import org.springframework.stereotype.Service;

@Service
class ActivityStructureService {

    private final ActivityRepository activityRepository;
    private final ActivityStructureRepository structureRepository;
    private final Clock clock;

    ActivityStructureService(ActivityRepository activityRepository, ActivityStructureRepository structureRepository) {
        this.activityRepository = activityRepository;
        this.structureRepository = structureRepository;
        this.clock = Clock.systemDefaultZone();
    }

    List<ProcessItemResponse> listProcessItems(Long activityId) {
        requireActivity(activityId);
        return structureRepository.listProcessItems(activityId);
    }

    ProcessItemResponse createProcessItem(Long activityId, ProcessItemRequest request, AdminPrincipal principal) {
        requireProcessEditable(activityId);
        return structureRepository.insertProcessItem(activityId, request, principal.id(), now());
    }

    ProcessItemResponse updateProcessItem(Long activityId, Long itemId, ProcessItemRequest request, AdminPrincipal principal) {
        requireProcessEditable(activityId);
        return structureRepository.updateProcessItem(activityId, itemId, request, principal.id(), now());
    }

    void deleteProcessItem(Long activityId, Long itemId, AdminPrincipal principal) {
        requireProcessEditable(activityId);
        structureRepository.deleteProcessItem(activityId, itemId, principal.id(), now());
    }

    List<CustomFieldResponse> listCustomFields(Long activityId) {
        requireActivity(activityId);
        return structureRepository.listCustomFields(activityId);
    }

    CustomFieldResponse createCustomField(Long activityId, CustomFieldRequest request, AdminPrincipal principal) {
        requireCustomFieldEditable(activityId);
        rejectDuplicateFieldKey(activityId, request.fieldKey(), null);
        return structureRepository.insertCustomField(activityId, request, JsonOptions.stringify(request.options()), principal.id(), now());
    }

    CustomFieldResponse updateCustomField(Long activityId, Long fieldId, CustomFieldRequest request, AdminPrincipal principal) {
        requireCustomFieldEditable(activityId);
        rejectDuplicateFieldKey(activityId, request.fieldKey(), fieldId);
        return structureRepository.updateCustomField(activityId, fieldId, request, JsonOptions.stringify(request.options()), principal.id(), now());
    }

    void deleteCustomField(Long activityId, Long fieldId, AdminPrincipal principal) {
        requireCustomFieldEditable(activityId);
        structureRepository.deleteCustomField(activityId, fieldId, principal.id(), now());
    }

    private ActivityRecord requireActivity(Long activityId) {
        return activityRepository.findById(activityId)
                .orElseThrow(() -> new NotFoundException("Activity not found"));
    }

    private void requireProcessEditable(Long activityId) {
        ActivityStatus status = ActivityStatus.valueOf(requireActivity(activityId).status());
        if (status == ActivityStatus.ENDED || status == ActivityStatus.ARCHIVED) {
            throw new InvalidStateException("Activity process items cannot be changed in " + status + " status");
        }
    }

    private void requireCustomFieldEditable(Long activityId) {
        ActivityStatus status = ActivityStatus.valueOf(requireActivity(activityId).status());
        if (status == ActivityStatus.IN_PROGRESS || status == ActivityStatus.ENDED || status == ActivityStatus.ARCHIVED) {
            throw new InvalidStateException("Registration custom fields cannot be changed in " + status + " status");
        }
    }

    private void rejectDuplicateFieldKey(Long activityId, String fieldKey, Long excludedFieldId) {
        if (structureRepository.customFieldKeyExists(activityId, fieldKey, excludedFieldId)) {
            throw new ConflictException("Custom field key already exists in this activity");
        }
    }

    private LocalDateTime now() {
        return LocalDateTime.now(clock);
    }
}
