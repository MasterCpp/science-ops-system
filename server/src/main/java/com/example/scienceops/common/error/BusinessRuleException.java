package com.example.scienceops.common.error;

public class BusinessRuleException extends RuntimeException {

    private final String code;
    private final int status;

    public BusinessRuleException(String code, String message, int status) {
        super(message);
        this.code = code;
        this.status = status;
    }

    public String code() {
        return code;
    }

    public int status() {
        return status;
    }
}
