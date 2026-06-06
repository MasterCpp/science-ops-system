package com.example.scienceops.mobile.activity;

import java.time.LocalDateTime;
import java.util.List;

import com.example.scienceops.admin.activity.CustomFieldResponse;
import com.example.scienceops.admin.activity.ProcessItemResponse;

public record PublicActivityDetailResponse(
        String id,
        String title,
        String coverFileId,
        String coverUrl,
        String description,
        LocalDateTime startTime,
        LocalDateTime endTime,
        String location,
        Integer capacity,
        Integer remainingCapacity,
        LocalDateTime registrationDeadline,
        String ownerName,
        String contactPhone,
        String status,
        String registrationAvailability,
        String registrationUnavailableReason,
        String registrationLink,
        String checkInLink,
        List<ProcessItemResponse> processItems,
        List<CustomFieldResponse> customFields
) {
}
