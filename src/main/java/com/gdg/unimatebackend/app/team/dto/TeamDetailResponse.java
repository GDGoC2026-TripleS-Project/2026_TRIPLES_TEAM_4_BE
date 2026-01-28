package com.gdg.unimatebackend.app.team.dto;

import com.gdg.unimatebackend.app.team.entity.TeamRole;
import lombok.Builder;
import lombok.Getter;

import java.util.List;

@Getter @Builder
public class TeamDetailResponse {
    private TeamResponse team;
    private List<TeamMemberResponse> members;
    private TeamRole myRole;
    private long memberCount;
}

