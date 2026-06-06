package com.example.scienceops.admin.volunteer;

import com.example.scienceops.common.api.ApiResponse;
import com.example.scienceops.common.api.PagedResponse;
import com.example.scienceops.security.AdminPrincipal;
import com.example.scienceops.volunteer.ManualVolunteerCheckInRequest;
import com.example.scienceops.volunteer.ManualVolunteerCheckOutRequest;
import com.example.scienceops.volunteer.VolunteerApplicationResponse;
import com.example.scienceops.volunteer.VolunteerApplicationReviewRequest;
import com.example.scienceops.volunteer.VolunteerAttendanceAdjustRequest;
import com.example.scienceops.volunteer.VolunteerAttendanceResponse;
import com.example.scienceops.volunteer.VolunteerPositionRequest;
import com.example.scienceops.volunteer.VolunteerPositionResponse;
import com.example.scienceops.volunteer.VolunteerService;
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
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/admin")
public class AdminVolunteerController {

    private final VolunteerService service;

    public AdminVolunteerController(VolunteerService service) {
        this.service = service;
    }

    @GetMapping("/activities/{activityId}/volunteer-positions")
    @PreAuthorize("hasAuthority('volunteer:manage')")
    public ApiResponse<java.util.List<VolunteerPositionResponse>> listPositions(@PathVariable Long activityId) {
        return ApiResponse.ok(service.listPositions(activityId));
    }

    @PostMapping("/activities/{activityId}/volunteer-positions")
    @PreAuthorize("hasAuthority('volunteer:manage')")
    public ApiResponse<VolunteerPositionResponse> createPosition(
            @PathVariable Long activityId,
            @Valid @RequestBody VolunteerPositionRequest request,
            @AuthenticationPrincipal AdminPrincipal principal
    ) {
        return ApiResponse.ok(service.createPosition(activityId, request, principal));
    }

    @PutMapping("/volunteer-positions/{positionId}")
    @PreAuthorize("hasAuthority('volunteer:manage')")
    public ApiResponse<VolunteerPositionResponse> updatePosition(
            @PathVariable Long positionId,
            @Valid @RequestBody VolunteerPositionRequest request,
            @AuthenticationPrincipal AdminPrincipal principal
    ) {
        return ApiResponse.ok(service.updatePosition(positionId, request, principal));
    }

    @DeleteMapping("/volunteer-positions/{positionId}")
    @PreAuthorize("hasAuthority('volunteer:manage')")
    public ApiResponse<VolunteerPositionResponse> deletePosition(
            @PathVariable Long positionId,
            @AuthenticationPrincipal AdminPrincipal principal
    ) {
        return ApiResponse.ok(service.deletePosition(positionId, principal));
    }

    @GetMapping("/volunteer-applications")
    @PreAuthorize("hasAuthority('volunteer:manage')")
    public ApiResponse<PagedResponse<VolunteerApplicationResponse>> listApplications(
            @RequestParam(required = false) Long activityId,
            @RequestParam(required = false) Long positionId,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String status,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int pageSize
    ) {
        return ApiResponse.ok(service.listApplications(activityId, positionId, keyword, status, page, pageSize));
    }

    @PostMapping("/volunteer-applications/{applicationId}/approve")
    @PreAuthorize("hasAuthority('volunteer:manage')")
    public ApiResponse<VolunteerApplicationResponse> approve(
            @PathVariable Long applicationId,
            @RequestBody(required = false) VolunteerApplicationReviewRequest request,
            @AuthenticationPrincipal AdminPrincipal principal
    ) {
        return ApiResponse.ok(service.approve(applicationId, request, principal));
    }

    @PostMapping("/volunteer-applications/{applicationId}/reject")
    @PreAuthorize("hasAuthority('volunteer:manage')")
    public ApiResponse<VolunteerApplicationResponse> reject(
            @PathVariable Long applicationId,
            @Valid @RequestBody(required = false) VolunteerApplicationReviewRequest request,
            @AuthenticationPrincipal AdminPrincipal principal
    ) {
        return ApiResponse.ok(service.reject(applicationId, request, principal));
    }

    @PostMapping("/volunteer-applications/{applicationId}/cancel")
    @PreAuthorize("hasAuthority('volunteer:manage')")
    public ApiResponse<VolunteerApplicationResponse> cancel(
            @PathVariable Long applicationId,
            @Valid @RequestBody(required = false) VolunteerApplicationReviewRequest request,
            @AuthenticationPrincipal AdminPrincipal principal
    ) {
        return ApiResponse.ok(service.cancel(applicationId, request, principal));
    }

    @GetMapping("/volunteer-applications/export")
    @PreAuthorize("hasAuthority('volunteer:manage')")
    public ResponseEntity<byte[]> export(
            @RequestParam(required = false) Long activityId,
            @RequestParam(required = false) Long positionId,
            @RequestParam(required = false) String status
    ) {
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"volunteer-applications.csv\"")
                .contentType(MediaType.parseMediaType("text/csv; charset=UTF-8"))
                .body(service.exportCsv(activityId, positionId, status));
    }

    @GetMapping("/volunteer-attendance")
    @PreAuthorize("hasAuthority('volunteer:manage')")
    public ApiResponse<PagedResponse<VolunteerAttendanceResponse>> listAttendance(
            @RequestParam(required = false) Long activityId,
            @RequestParam(required = false) Long positionId,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String status,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int pageSize
    ) {
        return ApiResponse.ok(service.listAttendances(activityId, positionId, keyword, status, page, pageSize));
    }

    @PostMapping("/activities/{activityId}/volunteer-attendance/manual-check-in")
    @PreAuthorize("hasAuthority('volunteer:manage')")
    public ApiResponse<VolunteerAttendanceResponse> manualCheckIn(
            @PathVariable Long activityId,
            @Valid @RequestBody ManualVolunteerCheckInRequest request,
            @AuthenticationPrincipal AdminPrincipal principal
    ) {
        return ApiResponse.ok(service.manualCheckIn(activityId, request, principal));
    }

    @PostMapping("/volunteer-applications/{applicationId}/attendance/manual-check-out")
    @PreAuthorize("hasAuthority('volunteer:manage')")
    public ApiResponse<VolunteerAttendanceResponse> manualCheckOut(
            @PathVariable Long applicationId,
            @RequestBody(required = false) ManualVolunteerCheckOutRequest request,
            @AuthenticationPrincipal AdminPrincipal principal
    ) {
        return ApiResponse.ok(service.manualCheckOut(applicationId, request, principal));
    }

    @PostMapping("/volunteer-attendance/{attendanceId}/adjust")
    @PreAuthorize("hasAuthority('volunteer:manage')")
    public ApiResponse<VolunteerAttendanceResponse> adjustAttendance(
            @PathVariable Long attendanceId,
            @Valid @RequestBody VolunteerAttendanceAdjustRequest request,
            @AuthenticationPrincipal AdminPrincipal principal
    ) {
        return ApiResponse.ok(service.adjustAttendance(attendanceId, request, principal));
    }

    @PostMapping("/volunteer-attendance/{attendanceId}/revoke")
    @PreAuthorize("hasAuthority('volunteer:manage')")
    public ApiResponse<VolunteerAttendanceResponse> revokeAttendance(
            @PathVariable Long attendanceId,
            @AuthenticationPrincipal AdminPrincipal principal
    ) {
        return ApiResponse.ok(service.revokeAttendance(attendanceId, principal));
    }
}
