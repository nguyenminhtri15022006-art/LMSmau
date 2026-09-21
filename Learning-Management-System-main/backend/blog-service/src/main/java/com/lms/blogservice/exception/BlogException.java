package com.lms.blogservice.exception;

import lombok.Getter;

@Getter
public class BlogException extends RuntimeException {
    final ErrorCode errorCode;

    public BlogException(ErrorCode errorCode) {
        super(errorCode.getMessage());
        this.errorCode = errorCode;
    }
}