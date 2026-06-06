package com.example.scienceops.registration;

import java.time.LocalDateTime;

record RegistrationActivityRecord(
        Long id,
        String title,
        Integer capacity,
        LocalDateTime registrationDeadline,
        String status,
        long registeredAttendeeCount
) {
}
