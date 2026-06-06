package com.example.scienceops.mobile.activity;

import java.time.LocalDateTime;

record PublicActivityRecord(
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
        String status,
        long registeredAttendeeCount
) {
}
