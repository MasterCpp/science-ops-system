package com.example.scienceops.volunteer;

import java.time.LocalDateTime;

public record ManualVolunteerCheckOutRequest(
        LocalDateTime checkOutTime
) {
}
