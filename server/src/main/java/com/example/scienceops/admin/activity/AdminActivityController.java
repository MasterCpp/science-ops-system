package com.example.scienceops.admin.activity;

import java.time.LocalDateTime;

import com.example.scienceops.common.api.ApiResponse;
import com.example.scienceops.common.api.PagedResponse;
import com.example.scienceops.security.AdminPrincipal;
import jakarta.validation.Valid;
import org.springframework.format.annotation.DateTimeFormat;
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
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/admin/activities")
@PreAuthorize("hasAuthority('activity:manage')")
public class AdminActivityController {

    private final ActivityService service;

    public AdminActivityController(ActivityService service) {
        this.service = service;
    }

    @GetMapping
    public ApiResponse<PagedResponse<ActivityResponse>> list(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime startFrom,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime startTo,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int pageSize
    ) {
        return ApiResponse.ok(service.list(keyword, status, startFrom, startTo, page, pageSize));
    }

    @PostMapping
    public ApiResponse<ActivityResponse> create(
            @Valid @RequestBody ActivityRequest request,
            @AuthenticationPrincipal AdminPrincipal principal
    ) {
        return ApiResponse.ok(service.create(request, principal));
    }

    @GetMapping("/{activityId}")
    public ApiResponse<ActivityResponse> detail(@PathVariable Long activityId) {
        return ApiResponse.ok(service.detail(activityId));
    }

    @PutMapping("/{activityId}")
    public ApiResponse<ActivityResponse> update(
            @PathVariable Long activityId,
            @Valid @RequestBody ActivityRequest request,
            @AuthenticationPrincipal AdminPrincipal principal
    ) {
        return ApiResponse.ok(service.update(activityId, request, principal));
    }

    @DeleteMapping("/{activityId}")
    public ResponseEntity<ApiResponse<Object>> delete(
            @PathVariable Long activityId,
            @AuthenticationPrincipal AdminPrincipal principal
    ) {
        service.delete(activityId, principal);
        return ResponseEntity.ok(ApiResponse.ok(null));
    }

    @PostMapping("/{activityId}/publish")
    public ApiResponse<ActivityResponse> publish(@PathVariable Long activityId, @AuthenticationPrincipal AdminPrincipal principal) {
        return ApiResponse.ok(service.publish(activityId, principal));
    }

    @PostMapping("/{activityId}/start")
    public ApiResponse<ActivityResponse> start(@PathVariable Long activityId, @AuthenticationPrincipal AdminPrincipal principal) {
        return ApiResponse.ok(service.start(activityId, principal));
    }

    @PostMapping("/{activityId}/end")
    public ApiResponse<ActivityResponse> end(@PathVariable Long activityId, @AuthenticationPrincipal AdminPrincipal principal) {
        return ApiResponse.ok(service.end(activityId, principal));
    }

    @PostMapping("/{activityId}/archive")
    public ApiResponse<ActivityResponse> archive(@PathVariable Long activityId, @AuthenticationPrincipal AdminPrincipal principal) {
        return ApiResponse.ok(service.archive(activityId, principal));
    }

    @PostMapping("/{activityId}/unarchive")
    public ApiResponse<ActivityResponse> unarchive(@PathVariable Long activityId, @AuthenticationPrincipal AdminPrincipal principal) {
        return ApiResponse.ok(service.unarchive(activityId, principal));
    }
}
