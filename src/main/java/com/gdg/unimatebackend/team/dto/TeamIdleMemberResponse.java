package com.gdg.unimatebackend.team.dto;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class TeamIdleMemberResponse {
    private Long userId;
    private String nickname;
    private String profileImageUrl;
    private String displayColorHex;
}
