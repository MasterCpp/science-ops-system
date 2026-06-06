package com.example.scienceops.mobile.activity;

import java.time.Clock;
import java.time.LocalDateTime;

import com.example.scienceops.common.enums.ActivityStatus;
import com.example.scienceops.common.error.NotFoundException;
import org.springframework.stereotype.Service;

@Service
class PublicActivityService {

    private final PublicActivityRepository repository;
    private final Clock clock;

    PublicActivityService(PublicActivityRepository repository) {
        this.repository = repository;
        this.clock = Clock.systemDefaultZone();
    }

    PublicActivityDetailResponse detail(Long activityId) {
        PublicActivityRecord activity = repository.findActivity(activityId)
                .orElseThrow(() -> new NotFoundException("Activity not found"));
        Integer remainingCapacity = remainingCapacity(activity);
        Availability availability = availability(activity, remainingCapacity);
        return new PublicActivityDetailResponse(
                String.valueOf(activity.id()),
                activity.title(),
                activity.coverFileId() == null ? null : String.valueOf(activity.coverFileId()),
                activity.coverFileId() == null ? null : "/api/admin/files/" + activity.coverFileId() + "/preview",
                activity.description(),
                activity.startTime(),
                activity.endTime(),
                activity.location(),
                activity.capacity(),
                remainingCapacity,
                activity.registrationDeadline(),
                activity.ownerName(),
                activity.contactPhone(),
                activity.status(),
                availability.status(),
                availability.reason(),
                "/m/activities/" + activity.id(),
                "/m/activities/" + activity.id() + "/check-in",
                "/m/activities/" + activity.id() + "/volunteers",
                repository.listProcessItems(activityId),
                repository.listCustomFields(activityId)
        );
    }

    private Integer remainingCapacity(PublicActivityRecord activity) {
        if (activity.capacity() == null) {
            return null;
        }
        return Math.max(0, activity.capacity() - Math.toIntExact(activity.registeredAttendeeCount()));
    }

    private Availability availability(PublicActivityRecord activity, Integer remainingCapacity) {
        ActivityStatus status = ActivityStatus.valueOf(activity.status());
        if (status != ActivityStatus.REGISTRATION_OPEN) {
            return new Availability("CLOSED", "Activity is not open for registration");
        }
        if (activity.registrationDeadline() != null && LocalDateTime.now(clock).isAfter(activity.registrationDeadline())) {
            return new Availability("DEADLINE_PASSED", "Registration deadline has passed");
        }
        if (remainingCapacity != null && remainingCapacity <= 0) {
            return new Availability("CAPACITY_FULL", "Activity capacity is full");
        }
        return new Availability("OPEN", null);
    }

    private record Availability(String status, String reason) {
    }
}
