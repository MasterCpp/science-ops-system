package com.example.scienceops.admin.operationlog;

import java.time.LocalDateTime;

import com.example.scienceops.common.api.ApiResponse;
import com.example.scienceops.common.api.PagedResponse;
import com.example.scienceops.operationlog.OperationLogResponse;
import com.example.scienceops.operationlog.OperationLogService;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/admin/operation-logs")
@PreAuthorize("hasAuthority('operation-log:view')")
public class AdminOperationLogController {

    private final OperationLogService service;

    public AdminOperationLogController(OperationLogService service) {
        this.service = service;
    }

    @GetMapping
    public ApiResponse<PagedResponse<OperationLogResponse>> list(
            @RequestParam(required = false) Long adminUserId,
            @RequestParam(required = false) String action,
            @RequestParam(required = false) String targetType,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime createdFrom,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime createdTo,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int pageSize
    ) {
        return ApiResponse.ok(service.list(adminUserId, action, targetType, createdFrom, createdTo, page, pageSize));
    }

    @GetMapping("/{logId}")
    public ApiResponse<OperationLogResponse> detail(@PathVariable Long logId) {
        return ApiResponse.ok(service.detail(logId));
    }
}
