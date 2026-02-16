package com.gdg.unimatebackend.todo.exception;

import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
public class TodoException extends RuntimeException {

    private final String code;
    private final HttpStatus status;

    public TodoException(String code, HttpStatus status, String message) {
        super(message);
        this.code = code;
        this.status = status;
    }

    public static TodoException notTeamMember() {
        return new TodoException(
                TodoErrorCodes.NOT_TEAM_MEMBER_CODE,
                TodoErrorCodes.FORBIDDEN,
                TodoErrorCodes.NOT_TEAM_MEMBER_MSG
        );
    }

    public static TodoException todoNotFound() {
        return new TodoException(
                TodoErrorCodes.TODO_NOT_FOUND_CODE,
                TodoErrorCodes.NOT_FOUND,
                TodoErrorCodes.TODO_NOT_FOUND_MSG
        );
    }

    public static TodoException duplicateTodo() {
        return new TodoException(
                TodoErrorCodes.DUPLICATE_TODO_CODE,
                TodoErrorCodes.BAD_REQUEST,
                TodoErrorCodes.DUPLICATE_TODO_MSG
        );
    }

    public static TodoException invalidDate() {
        return new TodoException(
                TodoErrorCodes.INVALID_DATE_CODE,
                TodoErrorCodes.BAD_REQUEST,
                TodoErrorCodes.INVALID_DATE_MSG
        );
    }
}