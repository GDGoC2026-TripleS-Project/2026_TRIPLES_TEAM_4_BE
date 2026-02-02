package com.gdg.unimatebackend.app.schedule.exception;

import lombok.Getter;

@Getter
public class MyScheduleException extends RuntimeException {
    private final String code;
    private final int status;

    public MyScheduleException(String code, String message, int status) {
        super(message);
        this.code = code;
        this.status = status;
    }
}
