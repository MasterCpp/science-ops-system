package com.example.scienceops.admin.activity;

import java.time.LocalDateTime;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;

public record ActivityRequest(
        @NotBlank @Size(max = 200) String title,
        Long coverFileId,
        String description,
        @NotNull LocalDateTime startTime,
        @NotNull LocalDateTime endTime,
        @NotBlank @Size(max = 255) String location,
        @PositiveOrZero Integer capacity,
        LocalDateTime registrationDeadline,
        @Size(max = 64) String ownerName,
        @Size(max = 32) String contactPhone,
        String planContent
) {
}
