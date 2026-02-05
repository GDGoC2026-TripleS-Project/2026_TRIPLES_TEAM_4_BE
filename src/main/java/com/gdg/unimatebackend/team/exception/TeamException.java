package com.gdg.unimatebackend.team.exception;

import lombok.Getter;

@Getter
public class TeamException extends RuntimeException {
    private final String code;
    private final int status;

    public TeamException(String code, String message, int status) {
        super(message);
        this.code = code;
        this.status = status;
    }
}
