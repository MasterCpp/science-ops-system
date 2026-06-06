package com.example.scienceops.admin.auth;

import com.example.scienceops.common.api.ApiResponse;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class AdminAuthExceptionHandler {

    @ExceptionHandler(AdminAuthService.LoginFailedException.class)
    public ResponseEntity<ApiResponse<Object>> loginFailed(AdminAuthService.LoginFailedException exception) {
        return ResponseEntity.status(401).body(ApiResponse.error("UNAUTHORIZED", exception.getMessage()));
    }

    @ExceptionHandler(AdminAuthService.DisabledAdminException.class)
    public ResponseEntity<ApiResponse<Object>> disabled(AdminAuthService.DisabledAdminException exception) {
        return ResponseEntity.status(403).body(ApiResponse.error("FORBIDDEN", exception.getMessage()));
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiResponse<Object>> validationFailed(MethodArgumentNotValidException exception) {
        return ResponseEntity.badRequest().body(ApiResponse.error("VALIDATION_ERROR", "Request validation failed"));
    }
}
