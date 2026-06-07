package com.example.scienceops.fileasset;

import java.time.LocalDateTime;

public record FileAssetResponse(
        String id,
        String activityId,
        String category,
        String originalName,
        String mimeType,
        String extension,
        Long sizeBytes,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
}
