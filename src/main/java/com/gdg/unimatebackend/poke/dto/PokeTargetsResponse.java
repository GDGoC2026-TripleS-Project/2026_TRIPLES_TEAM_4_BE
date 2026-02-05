package com.gdg.unimatebackend.poke.dto;

import lombok.Builder;
import lombok.Getter;

import java.util.List;

@Getter
@Builder
public class PokeTargetsResponse {

    private List<TeamSection> teams;

    @Getter
    @Builder
    public static class TeamSection {
        private Long teamId;
        private String teamName;
        private List<Member> members;
    }

    @Getter
    @Builder
    public static class Member {
        private Long userId;
        private String nickname;
        private String profileImageUrl;
    }
}
