package com.example.scienceops.admin.visitorreport;

import java.time.LocalDate;
import java.util.LinkedHashMap;
import java.util.Map;

import com.example.scienceops.common.api.ApiResponse;
import com.example.scienceops.common.api.PagedResponse;
import com.example.scienceops.operationlog.OperationLogService;
import com.example.scienceops.security.AdminPrincipal;
import com.example.scienceops.visitorreport.VisitorReportRequest;
import com.example.scienceops.visitorreport.VisitorReportResponse;
import com.example.scienceops.visitorreport.VisitorReportService;
import jakarta.validation.Valid;
import org.springframework.format.annotation.DateTimeFormat;
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
@RequestMapping("/api/admin/visitor-reports")
@PreAuthorize("hasAuthority('visitor-report:manage')")
public class AdminVisitorReportController {

    private final VisitorReportService service;
    private final OperationLogService operationLogService;

    public AdminVisitorReportController(VisitorReportService service, OperationLogService operationLogService) {
        this.service = service;
        this.operationLogService = operationLogService;
    }

    @GetMapping
    public ApiResponse<PagedResponse<VisitorReportResponse>> list(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) Long activityId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate visitFrom,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate visitTo,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int pageSize
    ) {
        return ApiResponse.ok(service.list(keyword, activityId, visitFrom, visitTo, page, pageSize));
    }

    @PostMapping
    public ApiResponse<VisitorReportResponse> create(
            @Valid @RequestBody VisitorReportRequest request,
            @AuthenticationPrincipal AdminPrincipal principal
    ) {
        return ApiResponse.ok(service.create(request, principal));
    }

    @GetMapping("/{visitorReportId}")
    public ApiResponse<VisitorReportResponse> detail(@PathVariable Long visitorReportId) {
        return ApiResponse.ok(service.detail(visitorReportId));
    }

    @PutMapping("/{visitorReportId}")
    public ApiResponse<VisitorReportResponse> update(
            @PathVariable Long visitorReportId,
            @Valid @RequestBody VisitorReportRequest request,
            @AuthenticationPrincipal AdminPrincipal principal
    ) {
        return ApiResponse.ok(service.update(visitorReportId, request, principal));
    }

    @DeleteMapping("/{visitorReportId}")
    public ResponseEntity<ApiResponse<Object>> delete(
            @PathVariable Long visitorReportId,
            @AuthenticationPrincipal AdminPrincipal principal
    ) {
        service.delete(visitorReportId, principal);
        return ResponseEntity.ok(ApiResponse.ok(null));
    }

    @GetMapping("/export")
    public ResponseEntity<byte[]> export(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) Long activityId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate visitFrom,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate visitTo,
            @AuthenticationPrincipal AdminPrincipal principal
    ) {
        byte[] csv = service.exportCsv(keyword, activityId, visitFrom, visitTo);
        operationLogService.record(principal, "VISITOR_REPORT_EXPORT", "VISITOR_REPORT", activityId, "Visitor reports", exportDetails(keyword, activityId, visitFrom, visitTo));
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"visitor-reports.csv\"")
                .contentType(MediaType.parseMediaType("text/csv; charset=UTF-8"))
                .body(csv);
    }

    private Map<String, Object> exportDetails(String keyword, Long activityId, LocalDate visitFrom, LocalDate visitTo) {
        Map<String, Object> details = new LinkedHashMap<>();
        details.put("keyword", keyword);
        details.put("activityId", activityId);
        details.put("visitFrom", visitFrom == null ? null : visitFrom.toString());
        details.put("visitTo", visitTo == null ? null : visitTo.toString());
        return details;
    }
}
