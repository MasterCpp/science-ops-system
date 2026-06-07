package com.example.scienceops.operationlog;

import java.time.Clock;
import java.time.LocalDateTime;
import java.util.Map;

import com.baomidou.mybatisplus.core.toolkit.IdWorker;
import com.example.scienceops.common.api.PagedResponse;
import com.example.scienceops.common.error.NotFoundException;
import com.example.scienceops.security.AdminPrincipal;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.stereotype.Service;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

@Service
public class OperationLogService {

    private final OperationLogRepository repository;
    private final ObjectMapper objectMapper;
    private final Clock clock;

    public OperationLogService(OperationLogRepository repository, ObjectMapper objectMapper) {
        this.repository = repository;
        this.objectMapper = objectMapper;
        this.clock = Clock.systemDefaultZone();
    }

    public void record(
            AdminPrincipal principal,
            String action,
            String targetType,
            Long targetId,
            String targetSummary,
            Map<String, Object> details
    ) {
        RequestMetadata requestMetadata = currentRequestMetadata();
        repository.insert(
                IdWorker.getId(),
                principal.id(),
                principal.username(),
                principal.roles().stream().findFirst().orElse("UNKNOWN"),
                action,
                targetType,
                targetId,
                truncate(targetSummary, 255),
                truncate(requestMetadata.ip(), 64),
                truncate(requestMetadata.userAgent(), 500),
                toJson(details),
                LocalDateTime.now(clock)
        );
    }

    public PagedResponse<OperationLogResponse> list(
            Long adminUserId,
            String action,
            String targetType,
            LocalDateTime createdFrom,
            LocalDateTime createdTo,
            int page,
            int pageSize
    ) {
        int safePage = Math.max(page, 1);
        int safePageSize = Math.max(1, Math.min(pageSize, 100));
        return new PagedResponse<>(
                repository.list(adminUserId, action, targetType, createdFrom, createdTo, safePage, safePageSize)
                        .stream()
                        .map(this::toResponse)
                        .toList(),
                safePage,
                safePageSize,
                repository.count(adminUserId, action, targetType, createdFrom, createdTo)
        );
    }

    public OperationLogResponse detail(Long logId) {
        return repository.findById(logId)
                .map(this::toResponse)
                .orElseThrow(() -> new NotFoundException("Operation log not found"));
    }

    private OperationLogResponse toResponse(OperationLogRecord record) {
        return new OperationLogResponse(
                String.valueOf(record.id()),
                record.adminUserId() == null ? null : String.valueOf(record.adminUserId()),
                record.adminUsername(),
                record.adminRoleCode(),
                record.action(),
                record.targetType(),
                record.targetId() == null ? null : String.valueOf(record.targetId()),
                record.targetSummary(),
                record.ip(),
                record.userAgent(),
                record.detailJson(),
                record.createdAt()
        );
    }

    private RequestMetadata currentRequestMetadata() {
        if (!(RequestContextHolder.getRequestAttributes() instanceof ServletRequestAttributes attributes)) {
            return new RequestMetadata(null, null);
        }
        HttpServletRequest request = attributes.getRequest();
        String forwardedFor = request.getHeader("X-Forwarded-For");
        String ip = forwardedFor == null || forwardedFor.isBlank()
                ? request.getRemoteAddr()
                : forwardedFor.split(",")[0].trim();
        return new RequestMetadata(ip, request.getHeader("User-Agent"));
    }

    private String toJson(Map<String, Object> details) {
        try {
            return objectMapper.writeValueAsString(details == null ? Map.of() : details);
        } catch (JsonProcessingException exception) {
            throw new IllegalStateException("Failed to serialize operation log details", exception);
        }
    }

    private String truncate(String value, int maxLength) {
        if (value == null || value.length() <= maxLength) {
            return value;
        }
        return value.substring(0, maxLength);
    }

    private record RequestMetadata(String ip, String userAgent) {
    }
}
