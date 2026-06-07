package com.example.scienceops.admin.activity;

import java.time.LocalDateTime;
import java.math.BigDecimal;

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
        long checkedInCount,
        BigDecimal checkInRate,
        long volunteerApplicationCount,
        long approvedVolunteerCount,
        long totalServiceMinutes,
        long surveyResponseCount,
        BigDecimal averageRating,
        long photoCount
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
                checkInRate,
                volunteerApplicationCount,
                approvedVolunteerCount,
                totalServiceMinutes,
                surveyResponseCount,
                averageRating,
                photoCount,
                createdAt,
                updatedAt
        );
    }
}
