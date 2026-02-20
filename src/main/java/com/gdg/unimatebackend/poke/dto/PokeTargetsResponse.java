package com.gdg.unimatebackend.poke.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Getter;

import java.util.List;

@Getter
@Builder
@Schema(description = "찌르기 대상 조회 응답")
public class PokeTargetsResponse {

    @Schema(description = "내가 속한 팀별 섹션 목록")
    private List<TeamSection> teams;

    @Getter
    @Builder
    @Schema(description = "팀 단위 대상 섹션")
    public static class TeamSection {
        @Schema(description = "팀 ID", example = "12")
        private Long teamId;
        @Schema(description = "팀 이름", example = "체리시")
        private String teamName;
        @Schema(description = "해당 팀에서 찌를 수 있는 대상(본인 제외)")
        private List<Member> members;
    }

    @Getter
    @Builder
    @Schema(description = "찌르기 대상 사용자")
    public static class Member {
        @Schema(description = "유저 ID", example = "101")
        private Long userId;
        @Schema(description = "닉네임", example = "모니모")
        private String nickname;
        @Schema(description = "프로필 이미지 URL", example = "https://cdn.example.com/profile/101.jpg")
        private String profileImageUrl;
    }
}
