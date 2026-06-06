package com.example.scienceops.admin.activity;

import java.util.List;

import com.example.scienceops.common.api.ApiResponse;
import com.example.scienceops.security.AdminPrincipal;
import jakarta.validation.Valid;
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
@RequestMapping("/api/admin/activities/{activityId}")
@PreAuthorize("hasAuthority('activity:manage')")
public class AdminActivityStructureController {

    private final ActivityStructureService service;

    public AdminActivityStructureController(ActivityStructureService service) {
        this.service = service;
    }

    @GetMapping("/process-items")
    public ApiResponse<List<ProcessItemResponse>> listProcessItems(@PathVariable Long activityId) {
        return ApiResponse.ok(service.listProcessItems(activityId));
    }

    @PostMapping("/process-items")
    public ApiResponse<ProcessItemResponse> createProcessItem(
            @PathVariable Long activityId,
            @Valid @RequestBody ProcessItemRequest request,
            @AuthenticationPrincipal AdminPrincipal principal
    ) {
        return ApiResponse.ok(service.createProcessItem(activityId, request, principal));
    }

    @PutMapping("/process-items/{itemId}")
    public ApiResponse<ProcessItemResponse> updateProcessItem(
            @PathVariable Long activityId,
            @PathVariable Long itemId,
            @Valid @RequestBody ProcessItemRequest request,
            @AuthenticationPrincipal AdminPrincipal principal
    ) {
        return ApiResponse.ok(service.updateProcessItem(activityId, itemId, request, principal));
    }

    @DeleteMapping("/process-items/{itemId}")
    public ResponseEntity<ApiResponse<Object>> deleteProcessItem(
            @PathVariable Long activityId,
            @PathVariable Long itemId,
            @AuthenticationPrincipal AdminPrincipal principal
    ) {
        service.deleteProcessItem(activityId, itemId, principal);
        return ResponseEntity.ok(ApiResponse.ok(null));
    }

    @GetMapping("/custom-fields")
    public ApiResponse<List<CustomFieldResponse>> listCustomFields(@PathVariable Long activityId) {
        return ApiResponse.ok(service.listCustomFields(activityId));
    }

    @PostMapping("/custom-fields")
    public ApiResponse<CustomFieldResponse> createCustomField(
            @PathVariable Long activityId,
            @Valid @RequestBody CustomFieldRequest request,
            @AuthenticationPrincipal AdminPrincipal principal
    ) {
        return ApiResponse.ok(service.createCustomField(activityId, request, principal));
    }

    @PutMapping("/custom-fields/{fieldId}")
    public ApiResponse<CustomFieldResponse> updateCustomField(
            @PathVariable Long activityId,
            @PathVariable Long fieldId,
            @Valid @RequestBody CustomFieldRequest request,
            @AuthenticationPrincipal AdminPrincipal principal
    ) {
        return ApiResponse.ok(service.updateCustomField(activityId, fieldId, request, principal));
    }

    @DeleteMapping("/custom-fields/{fieldId}")
    public ResponseEntity<ApiResponse<Object>> deleteCustomField(
            @PathVariable Long activityId,
            @PathVariable Long fieldId,
            @AuthenticationPrincipal AdminPrincipal principal
    ) {
        service.deleteCustomField(activityId, fieldId, principal);
        return ResponseEntity.ok(ApiResponse.ok(null));
    }
}
