package com.gdg.unimatebackend.calendar.exception;

public final class CalendarErrorCodes {

    private CalendarErrorCodes() {}

    public static final String INVALID_MONTH_FORMAT =
            "month 형식이 올바르지 않습니다. (예: 2026-02)";

    public static final String INVALID_DATE_FORMAT =
            "date 형식이 올바르지 않습니다. (예: 2026-02-10)";

    public static final String NO_ACCESSIBLE_TEAM =
            "조회 가능한 팀이 없습니다.";

    public static final String FORBIDDEN_TEAM_ACCESS =
            "해당 팀에 접근 권한이 없습니다.";
}