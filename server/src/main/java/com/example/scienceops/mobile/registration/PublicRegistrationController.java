package com.example.scienceops.mobile.registration;

import com.example.scienceops.common.api.ApiResponse;
import com.example.scienceops.registration.RegistrationRequest;
import com.example.scienceops.registration.RegistrationResponse;
import com.example.scienceops.registration.RegistrationService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/mobile/activities/{activityId}/registrations")
public class PublicRegistrationController {

    private final RegistrationService service;

    public PublicRegistrationController(RegistrationService service) {
        this.service = service;
    }

    @PostMapping
    public ApiResponse<RegistrationResponse> submit(
            @PathVariable Long activityId,
            @Valid @RequestBody RegistrationRequest request
    ) {
        return ApiResponse.ok(service.submitPublic(activityId, request));
    }
}
