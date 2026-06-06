package com.example.scienceops.admin.rbac;

import java.util.Map;

import com.example.scienceops.common.api.ApiResponse;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/admin")
public class RbacProbeController {

    @GetMapping("/users")
    @PreAuthorize("hasAuthority('admin-user:manage')")
    public ApiResponse<Map<String, Object>> users() {
        return probe("admin-user:manage");
    }

    @GetMapping("/operation-logs")
    @PreAuthorize("hasAuthority('operation-log:view')")
    public ApiResponse<Map<String, Object>> operationLogs() {
        return probe("operation-log:view");
    }

    @GetMapping("/rbac/probes/registration")
    @PreAuthorize("hasAuthority('registration:manage')")
    public ApiResponse<Map<String, Object>> registration() {
        return probe("registration:manage");
    }

    @GetMapping("/rbac/probes/survey")
    @PreAuthorize("hasAuthority('survey:manage')")
    public ApiResponse<Map<String, Object>> survey() {
        return probe("survey:manage");
    }

    @GetMapping("/rbac/probes/visitor-report")
    @PreAuthorize("hasAuthority('visitor-report:manage')")
    public ApiResponse<Map<String, Object>> visitorReport() {
        return probe("visitor-report:manage");
    }

    @GetMapping("/rbac/probes/file")
    @PreAuthorize("hasAuthority('file:manage')")
    public ApiResponse<Map<String, Object>> file() {
        return probe("file:manage");
    }

    private ApiResponse<Map<String, Object>> probe(String permission) {
        return ApiResponse.ok(Map.of("permission", permission));
    }
}
