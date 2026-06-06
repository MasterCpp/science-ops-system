package com.example.scienceops.mobile.volunteer;

import com.example.scienceops.common.api.ApiResponse;
import com.example.scienceops.volunteer.VolunteerApplicationRequest;
import com.example.scienceops.volunteer.VolunteerApplicationResponse;
import com.example.scienceops.volunteer.VolunteerAttendanceLookupRequest;
import com.example.scienceops.volunteer.VolunteerAttendanceResponse;
import com.example.scienceops.volunteer.VolunteerAttendanceStatusResponse;
import com.example.scienceops.volunteer.VolunteerPositionResponse;
import com.example.scienceops.volunteer.VolunteerService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/mobile/activities/{activityId}")
public class PublicVolunteerController {

    private final VolunteerService service;

    public PublicVolunteerController(VolunteerService service) {
        this.service = service;
    }

    @GetMapping("/volunteer-positions")
    public ApiResponse<java.util.List<VolunteerPositionResponse>> positions(@PathVariable Long activityId) {
        return ApiResponse.ok(service.listPositions(activityId));
    }

    @PostMapping("/volunteer-applications")
    public ApiResponse<VolunteerApplicationResponse> apply(
            @PathVariable Long activityId,
            @Valid @RequestBody VolunteerApplicationRequest request
    ) {
        return ApiResponse.ok(service.submitPublic(activityId, request));
    }

    @PostMapping("/volunteer-applications/status")
    public ApiResponse<VolunteerAttendanceStatusResponse> status(
            @PathVariable Long activityId,
            @Valid @RequestBody VolunteerAttendanceLookupRequest request
    ) {
        return ApiResponse.ok(service.lookupAttendanceStatus(activityId, request.phone()));
    }

    @PostMapping("/volunteer-applications/{applicationId}/check-in")
    public ApiResponse<VolunteerAttendanceResponse> checkIn(@PathVariable Long applicationId) {
        return ApiResponse.ok(service.publicCheckIn(applicationId));
    }

    @PostMapping("/volunteer-applications/{applicationId}/check-out")
    public ApiResponse<VolunteerAttendanceResponse> checkOut(@PathVariable Long applicationId) {
        return ApiResponse.ok(service.publicCheckOut(applicationId));
    }
}
