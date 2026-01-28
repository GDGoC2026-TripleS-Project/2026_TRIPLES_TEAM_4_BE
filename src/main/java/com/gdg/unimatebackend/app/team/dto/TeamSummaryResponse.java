package com.gdg.unimatebackend.app.team.dto;

import com.gdg.unimatebackend.app.team.entity.TeamColor;
import com.gdg.unimatebackend.app.team.entity.TeamRole;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@Builder
public class TeamSummaryResponse {
    private Long id;
    private String name;
    private String description;
    private TeamColor color;
    private String colorHex;
    private TeamRole myRole;
    private long memberCount;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
