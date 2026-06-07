package com.example.scienceops.admin.activity;

import java.time.LocalDateTime;
import java.math.BigDecimal;

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
        BigDecimal checkInRate,
        long volunteerApplicationCount,
        long approvedVolunteerCount,
        long totalServiceMinutes,
        long surveyResponseCount,
        BigDecimal averageRating,
        long photoCount,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
}
