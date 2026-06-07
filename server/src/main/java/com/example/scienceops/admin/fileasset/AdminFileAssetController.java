package com.example.scienceops.admin.fileasset;

import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Map;

import com.example.scienceops.common.api.ApiResponse;
import com.example.scienceops.common.api.PagedResponse;
import com.example.scienceops.fileasset.FileAssetResponse;
import com.example.scienceops.fileasset.FileAssetService;
import com.example.scienceops.fileasset.StoredFile;
import com.example.scienceops.operationlog.OperationLogService;
import com.example.scienceops.security.AdminPrincipal;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/admin")
@PreAuthorize("hasAuthority('file:manage')")
public class AdminFileAssetController {

    private final FileAssetService service;
    private final OperationLogService operationLogService;

    public AdminFileAssetController(FileAssetService service, OperationLogService operationLogService) {
        this.service = service;
        this.operationLogService = operationLogService;
    }

    @GetMapping("/activities/{activityId}/files")
    public ApiResponse<PagedResponse<FileAssetResponse>> list(
            @PathVariable Long activityId,
            @RequestParam(required = false) String category,
            @RequestParam(required = false) String keyword,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int pageSize
    ) {
        return ApiResponse.ok(service.list(activityId, category, keyword, page, pageSize));
    }

    @PostMapping(value = "/activities/{activityId}/files", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ApiResponse<FileAssetResponse> upload(
            @PathVariable Long activityId,
            @RequestParam String category,
            @RequestParam MultipartFile file,
            @AuthenticationPrincipal AdminPrincipal principal
    ) {
        return ApiResponse.ok(service.upload(activityId, category, file, principal));
    }

    @GetMapping("/files/{fileId}/preview")
    public ResponseEntity<Resource> preview(@PathVariable Long fileId) throws IOException {
        StoredFile storedFile = service.preview(fileId);
        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(storedFile.asset().mimeType()))
                .contentLength(storedFile.asset().sizeBytes())
                .body(new FileSystemResource(storedFile.path()));
    }

    @GetMapping("/files/{fileId}/download")
    public ResponseEntity<Resource> download(@PathVariable Long fileId) throws IOException {
        StoredFile storedFile = service.download(fileId);
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + encodedName(storedFile.asset().originalName()) + "\"")
                .contentType(MediaType.parseMediaType(storedFile.asset().mimeType()))
                .contentLength(storedFile.asset().sizeBytes())
                .body(new FileSystemResource(storedFile.path()));
    }

    @DeleteMapping("/files/{fileId}")
    public ResponseEntity<ApiResponse<Object>> delete(
            @PathVariable Long fileId,
            @AuthenticationPrincipal AdminPrincipal principal
    ) {
        service.delete(fileId, principal);
        return ResponseEntity.ok(ApiResponse.ok(null));
    }

    @GetMapping("/activities/{activityId}/photos.zip")
    public ResponseEntity<Resource> photoZip(
            @PathVariable Long activityId,
            @AuthenticationPrincipal AdminPrincipal principal
    ) {
        byte[] zip = service.photoZip(activityId);
        operationLogService.record(principal, "PHOTO_ZIP_EXPORT", "FILE_ASSET", activityId, "Activity photo archive", Map.of(
                "activityId", activityId
        ));
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"activity-" + activityId + "-photos.zip\"")
                .contentType(MediaType.parseMediaType("application/zip"))
                .contentLength(zip.length)
                .body(new ByteArrayResource(zip));
    }

    private String encodedName(String filename) {
        return URLEncoder.encode(filename, StandardCharsets.UTF_8).replace("+", "%20");
    }
}
