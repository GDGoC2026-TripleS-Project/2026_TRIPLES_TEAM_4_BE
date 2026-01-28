package com.gdg.unimatebackend.app.team.dto;

import com.gdg.unimatebackend.app.team.entity.TeamColor;
import com.gdg.unimatebackend.app.team.entity.TeamRole;
import lombok.Builder;
import lombok.Getter;

import java.util.List;

@Getter
@Builder
public class TeamJoinResponse {
    private TeamResponse team;
    private TeamRole myRole;
    private int memberCount;
    private List<TeamMemberResponse> members;
    private TeamColor myDisplayColor;
    private String myDisplayColorHex;
}
