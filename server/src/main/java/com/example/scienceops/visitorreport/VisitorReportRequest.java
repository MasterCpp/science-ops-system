package com.example.scienceops.visitorreport;

import java.time.LocalDateTime;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record VisitorReportRequest(
        Long activityId,
        @NotBlank @Size(max = 128) String visitorUnit,
        @NotBlank @Size(max = 64) String contactName,
        @NotBlank @Size(max = 32) String contactPhone,
        @NotNull @Min(1) Integer visitorCount,
        @NotNull LocalDateTime visitDate,
        @Size(max = 500) String visitReason,
        @Size(max = 500) String remark
) {
}
