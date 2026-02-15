package com.gdg.unimatebackend.home.exception;

import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
public class HomeException extends RuntimeException {

    private final String code;
    private final HttpStatus status;

    public HomeException(String code) {
        super(HomeErrorCodes.messageOf(code));
        this.code = code;
        this.status = HomeErrorCodes.statusOf(code);
    }
}