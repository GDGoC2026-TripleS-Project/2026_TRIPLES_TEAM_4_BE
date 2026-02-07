package com.gdg.unimatebackend.schedulepoll.exception;

public class SchedulePollException extends RuntimeException {

    private final SchedulePollErrorCodes.ErrorCode errorCode;

    public SchedulePollException(SchedulePollErrorCodes.ErrorCode errorCode) {
        super(errorCode.getMessage());
        this.errorCode = errorCode;
    }

    public SchedulePollException(SchedulePollErrorCodes.ErrorCode errorCode, String detailMessage) {
        super(detailMessage);
        this.errorCode = errorCode;
    }

    public SchedulePollErrorCodes.ErrorCode getErrorCode() {
        return errorCode;
    }
}
