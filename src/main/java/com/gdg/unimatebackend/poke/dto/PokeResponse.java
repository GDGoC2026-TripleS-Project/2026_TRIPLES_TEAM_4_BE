package com.gdg.unimatebackend.poke.dto;

import lombok.Builder;
import lombok.Getter;

import java.util.List;

@Getter
@Builder
public class PokeResponse {

    private int sentCount;
    private int excludedSelfCount;
    private List<InvalidTarget> invalidTargets;

    @Getter
    @Builder
    public static class InvalidTarget {
        private Long teamId;
        private Long userId;
        private String reason; // NOT_IN_MY_TEAM, NOT_TEAM_MEMBER
    }
}
