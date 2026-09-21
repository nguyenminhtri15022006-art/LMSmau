package com.lms.identityservice.exception;

public class IdentityException extends RuntimeException {
    private ErrorCode errorCode;

    public IdentityException(ErrorCode errorCode) {
        super(errorCode.getMessage());
        this.errorCode = errorCode;
    }

    public ErrorCode getErrorCode() {
        return errorCode;
    }

    public void setErrorCode(ErrorCode errorCode) {
        this.errorCode = errorCode;
    }
}
