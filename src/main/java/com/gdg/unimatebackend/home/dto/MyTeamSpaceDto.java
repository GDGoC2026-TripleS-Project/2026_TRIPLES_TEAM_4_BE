package com.gdg.unimatebackend.home.dto;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class MyTeamSpaceDto {
    private final Long teamId;
    private final String teamName;
    private final String teamColor;
    private final String teamProfileImageUrl; // 추가
}