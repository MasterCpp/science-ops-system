package com.example.scienceops.common.api;

import java.util.List;

public record PagedResponse<T>(
        List<T> items,
        int page,
        int pageSize,
        long total
) {
}
