package com.example.scienceops.fileasset;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Clock;
import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import com.baomidou.mybatisplus.core.toolkit.IdWorker;
import com.example.scienceops.common.api.PagedResponse;
import com.example.scienceops.common.enums.FileCategory;
import com.example.scienceops.common.error.BusinessRuleException;
import com.example.scienceops.common.error.NotFoundException;
import com.example.scienceops.operationlog.OperationLogService;
import com.example.scienceops.security.AdminPrincipal;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

@Service
public class FileAssetService {

    private static final long MB = 1024L * 1024L;
    private static final long COVER_MAX_BYTES = 10L * MB;
    private static final long PHOTO_MAX_BYTES = 10L * MB;
    private static final long ATTACHMENT_MAX_BYTES = 20L * MB;
    private static final Set<String> IMAGE_EXTENSIONS = Set.of("jpg", "jpeg", "png", "webp");
    private static final Set<String> ATTACHMENT_EXTENSIONS = Set.of(
            "pdf", "doc", "docx", "xls", "xlsx", "ppt", "pptx", "jpg", "jpeg", "png", "webp"
    );
    private static final Map<String, String> MIME_BY_EXTENSION = Map.ofEntries(
            Map.entry("jpg", "image/jpeg"),
            Map.entry("jpeg", "image/jpeg"),
            Map.entry("png", "image/png"),
            Map.entry("webp", "image/webp"),
            Map.entry("pdf", "application/pdf"),
            Map.entry("doc", "application/msword"),
            Map.entry("docx", "application/vnd.openxmlformats-officedocument.wordprocessingml.document"),
            Map.entry("xls", "application/vnd.ms-excel"),
            Map.entry("xlsx", "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"),
            Map.entry("ppt", "application/vnd.ms-powerpoint"),
            Map.entry("pptx", "application/vnd.openxmlformats-officedocument.presentationml.presentation")
    );

    private final FileAssetRepository repository;
    private final OperationLogService operationLogService;
    private final Clock clock;
    private final Path storageRoot;

    public FileAssetService(
            FileAssetRepository repository,
            OperationLogService operationLogService,
            @Value("${science-ops.storage.local-path}") String storageRoot
    ) {
        this.repository = repository;
        this.operationLogService = operationLogService;
        this.clock = Clock.systemDefaultZone();
        this.storageRoot = Path.of(storageRoot).toAbsolutePath().normalize();
    }

    public FileAssetResponse upload(Long activityId, String categoryValue, MultipartFile file, AdminPrincipal principal) {
        if (!repository.activityExists(activityId)) {
            throw new NotFoundException("Activity not found");
        }
        FileCategory category = parseCategory(categoryValue);
        String originalName = cleanOriginalName(file.getOriginalFilename());
        String extension = extension(originalName);
        validateType(category, extension);
        validateSize(category, file.getSize());

        Long id = IdWorker.getId();
        String storedName = id + "-" + UUID.randomUUID() + "." + extension;
        String relativePath = relativePath(activityId, category, storedName);
        Path target = resolveStoragePath(relativePath);
        try {
            Files.createDirectories(target.getParent());
            file.transferTo(target);
        } catch (IOException exception) {
            throw new IllegalStateException("Failed to store file", exception);
        }

        String mimeType = mimeType(file.getContentType(), extension);
        repository.insert(
                id,
                activityId,
                category.name(),
                originalName,
                storedName,
                mimeType,
                extension,
                file.getSize(),
                relativePath,
                principal.id(),
                LocalDateTime.now(clock)
        );
        return detail(id);
    }

    public PagedResponse<FileAssetResponse> list(Long activityId, String category, String keyword, int page, int pageSize) {
        if (!repository.activityExists(activityId)) {
            throw new NotFoundException("Activity not found");
        }
        String safeCategory = null;
        if (category != null && !category.isBlank()) {
            safeCategory = parseCategory(category).name();
        }
        int safePage = Math.max(page, 1);
        int safePageSize = Math.max(1, Math.min(pageSize, 100));
        return new PagedResponse<>(
                repository.list(activityId, safeCategory, keyword, safePage, safePageSize)
                        .stream()
                        .map(this::toResponse)
                        .toList(),
                safePage,
                safePageSize,
                repository.count(activityId, safeCategory, keyword)
        );
    }

    public FileAssetResponse detail(Long fileId) {
        return repository.findById(fileId)
                .map(this::toResponse)
                .orElseThrow(() -> new NotFoundException("File not found"));
    }

    public StoredFile preview(Long fileId) {
        FileAssetRecord asset = requireFile(fileId);
        if (!IMAGE_EXTENSIONS.contains(asset.extension().toLowerCase(Locale.ROOT))) {
            throw new BusinessRuleException("UNSUPPORTED_FILE_TYPE", "File preview is not supported", 415);
        }
        return new StoredFile(asset, requireExistingPath(asset));
    }

    public StoredFile download(Long fileId) {
        FileAssetRecord asset = requireFile(fileId);
        return new StoredFile(asset, requireExistingPath(asset));
    }

    public void delete(Long fileId, AdminPrincipal principal) {
        FileAssetRecord asset = requireFile(fileId);
        repository.delete(fileId, principal.id(), LocalDateTime.now(clock));
        try {
            Files.deleteIfExists(resolveStoragePath(asset.storagePath()));
        } catch (IOException exception) {
            throw new IllegalStateException("Failed to delete file", exception);
        }
        operationLogService.record(principal, "FILE_DELETE", "FILE_ASSET", asset.id(), asset.originalName(), fileDetails(asset));
    }

    public byte[] photoZip(Long activityId) {
        if (!repository.activityExists(activityId)) {
            throw new NotFoundException("Activity not found");
        }
        try {
            ByteArrayOutputStream bytes = new ByteArrayOutputStream();
            try (ZipOutputStream zip = new ZipOutputStream(bytes)) {
                for (FileAssetRecord photo : repository.listPhotosForZip(activityId)) {
                    Path path = resolveStoragePath(photo.storagePath());
                    if (!Files.exists(path)) {
                        continue;
                    }
                    zip.putNextEntry(new ZipEntry(zipEntryName(photo)));
                    Files.copy(path, zip);
                    zip.closeEntry();
                }
            }
            return bytes.toByteArray();
        } catch (IOException exception) {
            throw new IllegalStateException("Failed to create photo ZIP", exception);
        }
    }

    private FileCategory parseCategory(String categoryValue) {
        try {
            return FileCategory.valueOf(categoryValue);
        } catch (RuntimeException exception) {
            throw new BusinessRuleException("UNSUPPORTED_FILE_TYPE", "File category is not supported", 415);
        }
    }

    private void validateType(FileCategory category, String extension) {
        Set<String> allowed = switch (category) {
            case COVER, PHOTO -> IMAGE_EXTENSIONS;
            case ATTACHMENT -> ATTACHMENT_EXTENSIONS;
        };
        if (!allowed.contains(extension)) {
            throw new BusinessRuleException("UNSUPPORTED_FILE_TYPE", "File type is not supported", 415);
        }
    }

    private void validateSize(FileCategory category, long size) {
        long max = switch (category) {
            case COVER, PHOTO -> PHOTO_MAX_BYTES;
            case ATTACHMENT -> ATTACHMENT_MAX_BYTES;
        };
        if (size > max) {
            throw new BusinessRuleException("FILE_TOO_LARGE", "File is too large", 413);
        }
    }

    private String relativePath(Long activityId, FileCategory category, String storedName) {
        String directory = switch (category) {
            case COVER -> "covers";
            case ATTACHMENT -> "attachments";
            case PHOTO -> "photos";
        };
        return "activities/" + activityId + "/" + directory + "/" + storedName;
    }

    private Path resolveStoragePath(String relativePath) {
        Path resolved = storageRoot.resolve(relativePath).normalize();
        if (!resolved.startsWith(storageRoot)) {
            throw new IllegalStateException("Invalid storage path");
        }
        return resolved;
    }

    private Path requireExistingPath(FileAssetRecord asset) {
        Path path = resolveStoragePath(asset.storagePath());
        if (!Files.exists(path)) {
            throw new NotFoundException("File bytes not found");
        }
        return path;
    }

    private FileAssetRecord requireFile(Long fileId) {
        return repository.findById(fileId)
                .orElseThrow(() -> new NotFoundException("File not found"));
    }

    private String cleanOriginalName(String originalName) {
        if (originalName == null || originalName.isBlank()) {
            return "upload.bin";
        }
        return Path.of(originalName).getFileName().toString();
    }

    private String extension(String originalName) {
        int index = originalName.lastIndexOf('.');
        if (index < 0 || index == originalName.length() - 1) {
            throw new BusinessRuleException("UNSUPPORTED_FILE_TYPE", "File type is not supported", 415);
        }
        return originalName.substring(index + 1).toLowerCase(Locale.ROOT);
    }

    private String mimeType(String contentType, String extension) {
        if (contentType != null && !contentType.isBlank() && !"application/octet-stream".equals(contentType)) {
            return contentType;
        }
        return MIME_BY_EXTENSION.getOrDefault(extension, "application/octet-stream");
    }

    private String zipEntryName(FileAssetRecord asset) {
        String baseName = asset.id() + "-" + asset.originalName();
        return baseName.replace('\\', '_').replace('/', '_');
    }

    private Map<String, Object> fileDetails(FileAssetRecord asset) {
        Map<String, Object> details = new LinkedHashMap<>();
        details.put("activityId", asset.activityId());
        details.put("category", asset.category());
        details.put("sizeBytes", asset.sizeBytes());
        return details;
    }

    private FileAssetResponse toResponse(FileAssetRecord record) {
        return new FileAssetResponse(
                String.valueOf(record.id()),
                record.activityId() == null ? null : String.valueOf(record.activityId()),
                record.category(),
                record.originalName(),
                record.mimeType(),
                record.extension(),
                record.sizeBytes(),
                record.createdAt(),
                record.updatedAt()
        );
    }
}
