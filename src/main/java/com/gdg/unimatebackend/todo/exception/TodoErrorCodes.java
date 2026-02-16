package com.gdg.unimatebackend.todo.exception;

import org.springframework.http.HttpStatus;

public final class TodoErrorCodes {

    private TodoErrorCodes() {}

    public static final HttpStatus FORBIDDEN = HttpStatus.FORBIDDEN;
    public static final HttpStatus NOT_FOUND = HttpStatus.NOT_FOUND;
    public static final HttpStatus BAD_REQUEST = HttpStatus.BAD_REQUEST;

    public static final String NOT_TEAM_MEMBER_CODE = "TODO_403_001";
    public static final String NOT_TEAM_MEMBER_MSG = "해당 팀의 팀원이 아닙니다.";

    public static final String TODO_NOT_FOUND_CODE = "TODO_404_001";
    public static final String TODO_NOT_FOUND_MSG = "TODO를 찾을 수 없습니다.";

    public static final String DUPLICATE_TODO_CODE = "TODO_400_001";
    public static final String DUPLICATE_TODO_MSG = "같은 날짜에 동일한 TODO가 이미 존재합니다.";

    public static final String INVALID_DATE_CODE = "TODO_400_002";
    public static final String INVALID_DATE_MSG = "date는 필수입니다.";
}