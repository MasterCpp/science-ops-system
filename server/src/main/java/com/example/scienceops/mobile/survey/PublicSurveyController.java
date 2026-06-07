package com.example.scienceops.mobile.survey;

import com.example.scienceops.common.api.ApiResponse;
import com.example.scienceops.survey.SurveyEligibilityResponse;
import com.example.scienceops.survey.SurveyResponse;
import com.example.scienceops.survey.SurveyService;
import com.example.scienceops.survey.SurveySubmitRequest;
import com.example.scienceops.survey.SurveySubmitResponse;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/mobile/activities/{activityId}/survey")
public class PublicSurveyController {

    private final SurveyService service;

    public PublicSurveyController(SurveyService service) {
        this.service = service;
    }

    @GetMapping("/eligibility")
    public ApiResponse<SurveyEligibilityResponse> eligibility(
            @PathVariable Long activityId,
            @RequestParam String phone
    ) {
        return ApiResponse.ok(service.eligibility(activityId, phone));
    }

    @GetMapping
    public ApiResponse<SurveyResponse> detail(
            @PathVariable Long activityId,
            @RequestParam String phone
    ) {
        return ApiResponse.ok(service.publicSurvey(activityId, phone));
    }

    @PostMapping("/responses")
    public ApiResponse<SurveySubmitResponse> submit(
            @PathVariable Long activityId,
            @Valid @RequestBody SurveySubmitRequest request
    ) {
        return ApiResponse.ok(service.submit(activityId, request));
    }
}
