package com.gdg.unimatebackend.team.dto;

import com.gdg.unimatebackend.team.entity.TeamColor;
import com.gdg.unimatebackend.team.entity.TeamRole;
import lombok.Builder;
import lombok.Getter;

import java.util.List;

@Getter
@Builder
public class TeamDetailResponse {
    private TeamResponse team;
    private List<TeamMemberResponse> members;
    private TeamRole myRole;
    private int memberCount;
    private TeamColor myDisplayColor;
    private String myDisplayColorHex;
}
