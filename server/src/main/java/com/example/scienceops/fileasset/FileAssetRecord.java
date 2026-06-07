package com.example.scienceops.fileasset;

import java.time.LocalDateTime;

public record FileAssetRecord(
        Long id,
        Long activityId,
        String category,
        String originalName,
        String storedName,
        String mimeType,
        String extension,
        Long sizeBytes,
        String storagePath,
        Long uploadedBy,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
}
