package com.gdg.unimatebackend.global.exception;

/**
 * 카카오 API 호출 중 발생하는 예외
 */
public class KakaoApiException extends RuntimeException {

    private final int httpStatus;

    public KakaoApiException(String message) {
        super(message);
        this.httpStatus = 500;
    }

    public KakaoApiException(String message, int httpStatus) {
        super(message);
        this.httpStatus = httpStatus;
    }

    public KakaoApiException(String message, Throwable cause, int httpStatus) {
        super(message, cause);
        this.httpStatus = httpStatus;
    }

    public KakaoApiException(String message, Throwable cause) {
        super(message, cause);
        this.httpStatus = 500;
    }

    public int getHttpStatus() {
        return httpStatus;
    }
}

