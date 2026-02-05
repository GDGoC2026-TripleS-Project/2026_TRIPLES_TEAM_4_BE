package com.gdg.unimatebackend.team.exception;

public final class TeamErrorCodes {
    private TeamErrorCodes() {}

    public static final String TEAM_NOT_FOUND = "TEAM_NOT_FOUND";
    public static final String NOT_A_MEMBER = "NOT_A_MEMBER";
    public static final String FORBIDDEN = "FORBIDDEN";

    public static final String INVITE_CODE_INVALID = "INVITE_CODE_INVALID";
    public static final String INVITE_CODE_EXPIRED = "INVITE_CODE_EXPIRED";
    public static final String LEADER_CANNOT_LEAVE = "LEADER_CANNOT_LEAVE";
    public static final String INVITE_CODE_NOT_ISSUED = "INVITE_CODE_NOT_ISSUED";
}
