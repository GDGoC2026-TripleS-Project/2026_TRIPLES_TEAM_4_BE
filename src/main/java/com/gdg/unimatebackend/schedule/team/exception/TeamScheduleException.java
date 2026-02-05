package com.gdg.unimatebackend.schedule.team.exception;

import lombok.Getter;

@Getter
public class TeamScheduleException extends RuntimeException {
    private final String code;
    private final int status;

    public TeamScheduleException(String code, String message, int status) {
        super(message);
        this.code = code;
        this.status = status;
    }
}
