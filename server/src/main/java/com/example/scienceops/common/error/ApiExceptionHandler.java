package com.example.scienceops.common.error;

import com.example.scienceops.common.api.ApiResponse;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class ApiExceptionHandler {

    @ExceptionHandler(NotFoundException.class)
    public ResponseEntity<ApiResponse<Object>> notFound(NotFoundException exception) {
        return ResponseEntity.status(404).body(ApiResponse.error("NOT_FOUND", exception.getMessage()));
    }

    @ExceptionHandler(InvalidStateException.class)
    public ResponseEntity<ApiResponse<Object>> invalidState(InvalidStateException exception) {
        return ResponseEntity.status(409).body(ApiResponse.error("INVALID_STATE", exception.getMessage()));
    }

    @ExceptionHandler(ForbiddenException.class)
    public ResponseEntity<ApiResponse<Object>> forbidden(ForbiddenException exception) {
        return ResponseEntity.status(403).body(ApiResponse.error("FORBIDDEN", exception.getMessage()));
    }

    @ExceptionHandler(ConflictException.class)
    public ResponseEntity<ApiResponse<Object>> conflict(ConflictException exception) {
        return ResponseEntity.status(409).body(ApiResponse.error("CONFLICT", exception.getMessage()));
    }

    @ExceptionHandler(BusinessRuleException.class)
    public ResponseEntity<ApiResponse<Object>> businessRule(BusinessRuleException exception) {
        return ResponseEntity.status(exception.status()).body(ApiResponse.error(exception.code(), exception.getMessage()));
    }
}
