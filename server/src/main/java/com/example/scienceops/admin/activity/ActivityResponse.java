package com.example.scienceops.admin.activity;

import java.time.LocalDateTime;

public record ActivityResponse(
        String id,
        String title,
        String coverFileId,
        String description,
        LocalDateTime startTime,
        LocalDateTime endTime,
        String location,
        Integer capacity,
        LocalDateTime registrationDeadline,
        String ownerName,
        String contactPhone,
        String planContent,
        String status,
        long registeredAttendeeCount,
        long checkedInCount,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
}
