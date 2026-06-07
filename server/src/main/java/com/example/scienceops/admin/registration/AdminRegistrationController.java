package com.example.scienceops.admin.registration;

import java.util.Map;

import com.example.scienceops.common.api.ApiResponse;
import com.example.scienceops.common.api.PagedResponse;
import com.example.scienceops.operationlog.OperationLogService;
import com.example.scienceops.registration.RegistrationRequest;
import com.example.scienceops.registration.RegistrationResponse;
import com.example.scienceops.registration.RegistrationService;
import com.example.scienceops.security.AdminPrincipal;
import jakarta.validation.Valid;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/admin")
public class AdminRegistrationController {

    private final RegistrationService service;
    private final OperationLogService operationLogService;

    public AdminRegistrationController(RegistrationService service, OperationLogService operationLogService) {
        this.service = service;
        this.operationLogService = operationLogService;
    }

    @GetMapping("/activities/{activityId}/registrations")
    @PreAuthorize("hasAuthority('registration:manage')")
    public ApiResponse<PagedResponse<RegistrationResponse>> list(
            @PathVariable Long activityId,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String status,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int pageSize
    ) {
        return ApiResponse.ok(service.list(activityId, keyword, status, page, pageSize));
    }

    @PostMapping("/activities/{activityId}/registrations")
    @PreAuthorize("hasAuthority('registration:manage')")
    public ApiResponse<RegistrationResponse> backfill(
            @PathVariable Long activityId,
            @Valid @RequestBody RegistrationRequest request,
            @AuthenticationPrincipal AdminPrincipal principal
    ) {
        return ApiResponse.ok(service.backfill(activityId, request, principal));
    }

    @PostMapping("/registrations/{registrationId}/cancel")
    @PreAuthorize("hasAuthority('registration:manage')")
    public ApiResponse<RegistrationResponse> cancel(
            @PathVariable Long registrationId,
            @AuthenticationPrincipal AdminPrincipal principal
    ) {
        return ApiResponse.ok(service.cancel(registrationId, principal));
    }

    @GetMapping("/activities/{activityId}/registrations/export")
    @PreAuthorize("hasAuthority('registration:manage')")
    public ResponseEntity<byte[]> export(
            @PathVariable Long activityId,
            @AuthenticationPrincipal AdminPrincipal principal
    ) {
        byte[] csv = service.exportCsv(activityId);
        operationLogService.record(principal, "REGISTRATION_EXPORT", "REGISTRATION", activityId, "Activity registrations", Map.of(
                "activityId", activityId
        ));
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"registrations.csv\"")
                .contentType(MediaType.parseMediaType("text/csv; charset=UTF-8"))
                .body(csv);
    }
}
