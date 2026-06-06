package com.example.scienceops.admin.activity;

import java.util.List;

public record CustomFieldResponse(
        String id,
        String activityId,
        String fieldKey,
        String label,
        String fieldType,
        boolean required,
        List<String> options,
        int sortOrder
) {
}
