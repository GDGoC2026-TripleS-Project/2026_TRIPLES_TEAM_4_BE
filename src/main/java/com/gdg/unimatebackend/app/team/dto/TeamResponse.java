package com.gdg.unimatebackend.app.team.dto;

import com.gdg.unimatebackend.app.team.entity.TeamColor;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@Builder
public class TeamResponse {
    private Long id;
    private String name;
    private String description;
    private TeamColor color;
    private String colorHex;
    private Long ownerUserId;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
