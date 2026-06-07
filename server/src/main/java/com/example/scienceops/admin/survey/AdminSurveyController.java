package com.example.scienceops.admin.survey;

import java.util.Map;

import com.example.scienceops.common.api.ApiResponse;
import com.example.scienceops.common.api.PagedResponse;
import com.example.scienceops.operationlog.OperationLogService;
import com.example.scienceops.security.AdminPrincipal;
import com.example.scienceops.survey.SurveyOptionRequest;
import com.example.scienceops.survey.SurveyOptionResponse;
import com.example.scienceops.survey.SurveyQuestionRequest;
import com.example.scienceops.survey.SurveyQuestionResponse;
import com.example.scienceops.survey.SurveyRawResponse;
import com.example.scienceops.survey.SurveyRequest;
import com.example.scienceops.survey.SurveyResponse;
import com.example.scienceops.survey.SurveyService;
import com.example.scienceops.survey.SurveyStatisticsResponse;
import jakarta.validation.Valid;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/admin")
@PreAuthorize("hasAuthority('survey:manage')")
public class AdminSurveyController {

    private final SurveyService service;
    private final OperationLogService operationLogService;

    public AdminSurveyController(SurveyService service, OperationLogService operationLogService) {
        this.service = service;
        this.operationLogService = operationLogService;
    }

    @GetMapping("/activities/{activityId}/survey")
    public ApiResponse<SurveyResponse> getByActivity(@PathVariable Long activityId) {
        return ApiResponse.ok(service.getByActivity(activityId));
    }

    @PostMapping("/activities/{activityId}/survey")
    public ApiResponse<SurveyResponse> create(
            @PathVariable Long activityId,
            @Valid @RequestBody SurveyRequest request,
            @AuthenticationPrincipal AdminPrincipal principal
    ) {
        return ApiResponse.ok(service.create(activityId, request, principal));
    }

    @PutMapping("/surveys/{surveyId}")
    public ApiResponse<SurveyResponse> update(
            @PathVariable Long surveyId,
            @Valid @RequestBody SurveyRequest request,
            @AuthenticationPrincipal AdminPrincipal principal
    ) {
        return ApiResponse.ok(service.update(surveyId, request, principal));
    }

    @PostMapping("/surveys/{surveyId}/publish")
    public ApiResponse<SurveyResponse> publish(@PathVariable Long surveyId, @AuthenticationPrincipal AdminPrincipal principal) {
        return ApiResponse.ok(service.publish(surveyId, principal));
    }

    @PostMapping("/surveys/{surveyId}/close")
    public ApiResponse<SurveyResponse> close(@PathVariable Long surveyId, @AuthenticationPrincipal AdminPrincipal principal) {
        return ApiResponse.ok(service.close(surveyId, principal));
    }

    @PostMapping("/surveys/{surveyId}/questions")
    public ApiResponse<SurveyQuestionResponse> createQuestion(
            @PathVariable Long surveyId,
            @Valid @RequestBody SurveyQuestionRequest request
    ) {
        return ApiResponse.ok(service.createQuestion(surveyId, request));
    }

    @PutMapping("/survey-questions/{questionId}")
    public ApiResponse<SurveyQuestionResponse> updateQuestion(
            @PathVariable Long questionId,
            @Valid @RequestBody SurveyQuestionRequest request
    ) {
        return ApiResponse.ok(service.updateQuestion(questionId, request));
    }

    @DeleteMapping("/survey-questions/{questionId}")
    public ResponseEntity<ApiResponse<Object>> deleteQuestion(@PathVariable Long questionId) {
        service.deleteQuestion(questionId);
        return ResponseEntity.ok(ApiResponse.ok(null));
    }

    @PostMapping("/survey-questions/{questionId}/options")
    public ApiResponse<SurveyOptionResponse> createOption(
            @PathVariable Long questionId,
            @Valid @RequestBody SurveyOptionRequest request
    ) {
        return ApiResponse.ok(service.createOption(questionId, request));
    }

    @PutMapping("/survey-options/{optionId}")
    public ApiResponse<SurveyOptionResponse> updateOption(
            @PathVariable Long optionId,
            @Valid @RequestBody SurveyOptionRequest request
    ) {
        return ApiResponse.ok(service.updateOption(optionId, request));
    }

    @DeleteMapping("/survey-options/{optionId}")
    public ResponseEntity<ApiResponse<Object>> deleteOption(@PathVariable Long optionId) {
        service.deleteOption(optionId);
        return ResponseEntity.ok(ApiResponse.ok(null));
    }

    @GetMapping("/surveys/{surveyId}/statistics")
    public ApiResponse<SurveyStatisticsResponse> statistics(@PathVariable Long surveyId) {
        return ApiResponse.ok(service.statistics(surveyId));
    }

    @GetMapping("/surveys/{surveyId}/responses")
    public ApiResponse<PagedResponse<SurveyRawResponse>> responses(
            @PathVariable Long surveyId,
            @org.springframework.web.bind.annotation.RequestParam(defaultValue = "1") int page,
            @org.springframework.web.bind.annotation.RequestParam(defaultValue = "20") int pageSize
    ) {
        return ApiResponse.ok(service.responses(surveyId, page, pageSize));
    }

    @GetMapping("/surveys/{surveyId}/export")
    public ResponseEntity<byte[]> export(
            @PathVariable Long surveyId,
            @AuthenticationPrincipal AdminPrincipal principal
    ) {
        byte[] csv = service.exportCsv(surveyId);
        operationLogService.record(principal, "SURVEY_EXPORT", "SURVEY", surveyId, "Survey responses", Map.of(
                "surveyId", surveyId
        ));
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"survey-responses.csv\"")
                .contentType(MediaType.parseMediaType("text/csv; charset=UTF-8"))
                .body(csv);
    }
}
