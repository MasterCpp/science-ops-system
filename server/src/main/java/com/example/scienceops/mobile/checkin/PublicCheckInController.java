package com.example.scienceops.mobile.checkin;

import com.example.scienceops.checkin.CheckInRequest;
import com.example.scienceops.checkin.CheckInResponse;
import com.example.scienceops.checkin.CheckInService;
import com.example.scienceops.common.api.ApiResponse;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/mobile/activities/{activityId}/check-ins")
public class PublicCheckInController {

    private final CheckInService service;

    public PublicCheckInController(CheckInService service) {
        this.service = service;
    }

    @PostMapping
    public ApiResponse<CheckInResponse> checkIn(
            @PathVariable Long activityId,
            @Valid @RequestBody CheckInRequest request
    ) {
        return ApiResponse.ok(service.submitPublic(activityId, request));
    }
}
