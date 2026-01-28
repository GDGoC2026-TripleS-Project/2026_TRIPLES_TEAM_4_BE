package com.gdg.unimatebackend.app.team.dto;

import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@Builder
public class TeamInviteCodeResponse {
    private Long teamId;
    private String inviteCode;          // 숫자 6자리
    private LocalDateTime expiresAt;    // 발급 후 10분
}
