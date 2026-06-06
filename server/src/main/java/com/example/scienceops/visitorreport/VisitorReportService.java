package com.example.scienceops.visitorreport;

import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import com.baomidou.mybatisplus.core.toolkit.IdWorker;
import com.example.scienceops.common.api.PagedResponse;
import com.example.scienceops.common.error.NotFoundException;
import com.example.scienceops.security.AdminPrincipal;
import org.springframework.stereotype.Service;

@Service
public class VisitorReportService {

    private final VisitorReportRepository repository;
    private final Clock clock;

    public VisitorReportService(VisitorReportRepository repository) {
        this.repository = repository;
        this.clock = Clock.systemDefaultZone();
    }

    public VisitorReportResponse create(VisitorReportRequest request, AdminPrincipal principal) {
        validateActivity(request.activityId());
        Long id = IdWorker.getId();
        repository.insert(id, request, principal.id(), LocalDateTime.now(clock));
        return detail(id);
    }

    public VisitorReportResponse detail(Long visitorReportId) {
        return repository.findById(visitorReportId)
                .map(this::toResponse)
                .orElseThrow(() -> new NotFoundException("Visitor report not found"));
    }

    public VisitorReportResponse update(Long visitorReportId, VisitorReportRequest request, AdminPrincipal principal) {
        requireRecord(visitorReportId);
        validateActivity(request.activityId());
        repository.update(visitorReportId, request, principal.id(), LocalDateTime.now(clock));
        return detail(visitorReportId);
    }

    public void delete(Long visitorReportId, AdminPrincipal principal) {
        requireRecord(visitorReportId);
        repository.delete(visitorReportId, principal.id(), LocalDateTime.now(clock));
    }

    public PagedResponse<VisitorReportResponse> list(
            String keyword,
            Long activityId,
            LocalDate visitFrom,
            LocalDate visitTo,
            int page,
            int pageSize
    ) {
        int safePage = Math.max(page, 1);
        int safePageSize = Math.max(1, Math.min(pageSize, 100));
        return new PagedResponse<>(
                repository.list(keyword, activityId, visitFrom, visitTo, safePage, safePageSize)
                        .stream()
                        .map(this::toResponse)
                        .toList(),
                safePage,
                safePageSize,
                repository.count(keyword, activityId, visitFrom, visitTo)
        );
    }

    public byte[] exportCsv(String keyword, Long activityId, LocalDate visitFrom, LocalDate visitTo) {
        List<VisitorReportResponse> reports = repository.list(keyword, activityId, visitFrom, visitTo, 1, 10000)
                .stream()
                .map(this::toResponse)
                .toList();
        StringBuilder csv = new StringBuilder("\uFEFF");
        csv.append("ID,Visitor Unit,Contact Name,Contact Phone,Visitor Count,Visit Date,Visit Reason,Activity,Remark,Created At\n");
        for (VisitorReportResponse report : reports) {
            csv.append(csvCell(report.id())).append(',')
                    .append(csvCell(report.visitorUnit())).append(',')
                    .append(csvCell(report.contactName())).append(',')
                    .append(csvCell(report.contactPhone())).append(',')
                    .append(report.visitorCount()).append(',')
                    .append(csvCell(String.valueOf(report.visitDate()))).append(',')
                    .append(csvCell(report.visitReason())).append(',')
                    .append(csvCell(report.activityTitle())).append(',')
                    .append(csvCell(report.remark())).append(',')
                    .append(csvCell(String.valueOf(report.createdAt()))).append('\n');
        }
        return csv.toString().getBytes(StandardCharsets.UTF_8);
    }

    private VisitorReportRecord requireRecord(Long visitorReportId) {
        return repository.findById(visitorReportId)
                .orElseThrow(() -> new NotFoundException("Visitor report not found"));
    }

    private void validateActivity(Long activityId) {
        if (activityId != null && !repository.activityExists(activityId)) {
            throw new NotFoundException("Activity not found");
        }
    }

    private VisitorReportResponse toResponse(VisitorReportRecord record) {
        return new VisitorReportResponse(
                String.valueOf(record.id()),
                record.activityId() == null ? null : String.valueOf(record.activityId()),
                record.activityTitle(),
                record.visitorUnit(),
                record.contactName(),
                record.contactPhone(),
                record.visitorCount(),
                record.visitDate(),
                record.visitReason(),
                record.remark(),
                record.createdAt(),
                record.updatedAt()
        );
    }

    private String csvCell(String value) {
        String safeValue = value == null ? "" : value;
        return "\"" + safeValue.replace("\"", "\"\"") + "\"";
    }
}
