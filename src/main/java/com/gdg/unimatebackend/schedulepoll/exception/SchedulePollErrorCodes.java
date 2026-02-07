package com.gdg.unimatebackend.schedulepoll.exception;

import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
public final class SchedulePollErrorCodes {

    private SchedulePollErrorCodes() {}

    public static final ErrorCode SCHEDULE_POLL_NOT_FOUND =
            new ErrorCode(HttpStatus.NOT_FOUND, "SCHEDULE_POLL_404", "모임을 찾을 수 없습니다.");

    public static final ErrorCode FORBIDDEN =
            new ErrorCode(HttpStatus.FORBIDDEN, "SCHEDULE_POLL_403", "권한이 없습니다.");

    public static final ErrorCode POLL_LOCKED =
            new ErrorCode(HttpStatus.CONFLICT, "SCHEDULE_POLL_409", "확정된 모임은 변경할 수 없습니다.");

    public static final ErrorCode INVALID_DATE_RANGE =
            new ErrorCode(HttpStatus.BAD_REQUEST, "SCHEDULE_POLL_400_1", "날짜/시간 범위가 올바르지 않습니다.");

    public static final ErrorCode INVALID_SLOTS =
            new ErrorCode(HttpStatus.BAD_REQUEST, "SCHEDULE_POLL_400_2", "선택한 슬롯이 올바르지 않습니다.");

    public static final ErrorCode INVALID_REQUEST =
            new ErrorCode(HttpStatus.BAD_REQUEST, "SCHEDULE_POLL_400_3", "요청이 올바르지 않습니다.");

    @Getter
    public static class ErrorCode {
        private final HttpStatus httpStatus;
        private final String code;
        private final String message;

        public ErrorCode(HttpStatus httpStatus, String code, String message) {
            this.httpStatus = httpStatus;
            this.code = code;
            this.message = message;
        }
    }
}