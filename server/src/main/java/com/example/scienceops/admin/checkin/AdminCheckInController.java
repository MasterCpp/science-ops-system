package com.example.scienceops.admin.checkin;

import java.time.LocalDateTime;

import com.example.scienceops.checkin.CheckInResponse;
import com.example.scienceops.checkin.CheckInService;
import com.example.scienceops.checkin.ManualCheckInRequest;
import com.example.scienceops.common.api.ApiResponse;
import com.example.scienceops.common.api.PagedResponse;
import com.example.scienceops.security.AdminPrincipal;
import jakarta.validation.Valid;
import org.springframework.format.annotation.DateTimeFormat;
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
public class AdminCheckInController {

    private final CheckInService service;

    public AdminCheckInController(CheckInService service) {
        this.service = service;
    }

    @GetMapping("/activities/{activityId}/check-ins")
    @PreAuthorize("hasAuthority('check-in:manage')")
    public ApiResponse<PagedResponse<CheckInResponse>> list(
            @PathVariable Long activityId,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime checkedFrom,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime checkedTo,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int pageSize
    ) {
        return ApiResponse.ok(service.list(activityId, keyword, status, checkedFrom, checkedTo, page, pageSize));
    }

    @PostMapping("/activities/{activityId}/check-ins/manual")
    @PreAuthorize("hasAuthority('check-in:manage')")
    public ApiResponse<CheckInResponse> manualCheckIn(
            @PathVariable Long activityId,
            @Valid @RequestBody ManualCheckInRequest request,
            @AuthenticationPrincipal AdminPrincipal principal
    ) {
        return ApiResponse.ok(service.manualCheckIn(activityId, request, principal));
    }

    @PostMapping("/check-ins/{checkInId}/revoke")
    @PreAuthorize("hasAuthority('check-in:manage')")
    public ApiResponse<CheckInResponse> revoke(
            @PathVariable Long checkInId,
            @AuthenticationPrincipal AdminPrincipal principal
    ) {
        return ApiResponse.ok(service.revoke(checkInId, principal));
    }

    @GetMapping("/activities/{activityId}/check-ins/export")
    @PreAuthorize("hasAuthority('check-in:manage')")
    public ResponseEntity<byte[]> export(@PathVariable Long activityId) {
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"check-ins.csv\"")
                .contentType(MediaType.parseMediaType("text/csv; charset=UTF-8"))
                .body(service.exportCsv(activityId));
    }
}
