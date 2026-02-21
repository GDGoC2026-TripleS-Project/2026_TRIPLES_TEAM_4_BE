package com.gdg.unimatebackend.team.dto;

import com.gdg.unimatebackend.team.entity.TeamColor;
import com.gdg.unimatebackend.team.entity.TeamRole;
import com.fasterxml.jackson.annotation.JsonProperty;
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
    private String imageUrl;
    private TeamRole myRole;
    private long memberCount;
    private LocalDateTime startAt;
    private LocalDateTime endAt;
    @JsonProperty("isCompleted")
    private boolean isCompleted;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
