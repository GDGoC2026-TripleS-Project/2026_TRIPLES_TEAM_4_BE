package com.gdg.unimatebackend.team.dto;

import com.gdg.unimatebackend.team.entity.TeamColor;
import com.gdg.unimatebackend.team.entity.TeamRole;
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
