package com.example.scienceops.admin.activity;

import java.time.LocalDateTime;

record ActivityRecord(
        Long id,
        String title,
        Long coverFileId,
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
        LocalDateTime createdAt,
        LocalDateTime updatedAt,
        long registeredAttendeeCount,
        long checkedInCount
) {

    ActivityResponse toResponse() {
        return new ActivityResponse(
                String.valueOf(id),
                title,
                coverFileId == null ? null : String.valueOf(coverFileId),
                description,
                startTime,
                endTime,
                location,
                capacity,
                registrationDeadline,
                ownerName,
                contactPhone,
                planContent,
                status,
                registeredAttendeeCount,
                checkedInCount,
                createdAt,
                updatedAt
        );
    }
}
