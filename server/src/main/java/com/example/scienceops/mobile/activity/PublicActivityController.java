package com.example.scienceops.mobile.activity;

import com.example.scienceops.common.api.ApiResponse;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/mobile/activities")
public class PublicActivityController {

    private final PublicActivityService service;

    public PublicActivityController(PublicActivityService service) {
        this.service = service;
    }

    @GetMapping("/{activityId}")
    public ApiResponse<PublicActivityDetailResponse> detail(@PathVariable Long activityId) {
        return ApiResponse.ok(service.detail(activityId));
    }
}
