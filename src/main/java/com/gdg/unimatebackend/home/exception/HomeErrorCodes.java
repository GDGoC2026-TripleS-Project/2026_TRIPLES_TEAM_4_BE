package com.gdg.unimatebackend.home.exception;

import org.springframework.http.HttpStatus;

public final class HomeErrorCodes {
    private HomeErrorCodes() {}

    public static final String HOME_ERROR = "HOME_400_000";

    public static HttpStatus statusOf(String code) {
        return HttpStatus.BAD_REQUEST;
    }

    public static String messageOf(String code) {
        return "Home API 요청 처리 중 오류가 발생했습니다.";
    }
}