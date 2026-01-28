package com.gdg.unimatebackend.app.team.dto;

import com.gdg.unimatebackend.app.team.entity.TeamColor;
import com.gdg.unimatebackend.app.team.entity.TeamRole;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@Builder
public class TeamMemberResponse {
    private Long userId;
    private String nickname;
    private String profileImageUrl;
    private String universityName;
    private TeamRole role;
    private LocalDateTime joinedAt;
    private TeamColor displayColor;
    private String displayColorHex;
}
