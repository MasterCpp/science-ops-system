package com.example.scienceops.volunteer;

public record VolunteerAttendanceStatusResponse(
        VolunteerApplicationResponse application,
        VolunteerAttendanceResponse attendance
) {
}
