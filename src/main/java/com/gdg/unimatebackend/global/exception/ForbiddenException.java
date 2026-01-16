package com.gdg.unimatebackend.global.exception;

/**
 * 권한이 없는 작업을 시도했을 때 발생하는 예외 (HTTP 403)
 */
public class ForbiddenException extends RuntimeException {
    public ForbiddenException(String message) {
        super(message);
    }
}


