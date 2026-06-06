package com.example.scienceops.admin.activity;

public record ProcessItemResponse(
        String id,
        String activityId,
        String timeLabel,
        String title,
        String description,
        int sortOrder
) {
}
