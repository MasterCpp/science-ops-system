package com.example.scienceops.common.api;

import java.util.Map;

import com.fasterxml.jackson.annotation.JsonInclude;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record ApiResponse<T>(
        boolean success,
        T data,
        String message,
        String code,
        Map<String, Object> details
) {

    public static <T> ApiResponse<T> ok(T data) {
        return new ApiResponse<>(true, data, "OK", null, Map.of());
    }

    public static ApiResponse<Object> error(String code, String message) {
        return new ApiResponse<>(false, null, message, code, Map.of());
    }
}
