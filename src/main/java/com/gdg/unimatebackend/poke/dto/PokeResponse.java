package com.gdg.unimatebackend.poke.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Getter;

import java.util.List;

@Getter
@Builder
@Schema(description = "찌르기 전송 결과")
public class PokeResponse {

    @Schema(description = "정상 발송 건수", example = "2")
    private int sentCount;
    @Schema(description = "본인 대상이어서 제외된 건수", example = "1")
    private int excludedSelfCount;
    @Schema(description = "검증 실패 대상 목록")
    private List<InvalidTarget> invalidTargets;

    @Getter
    @Builder
    @Schema(description = "유효하지 않은 대상 정보")
    public static class InvalidTarget {
        @Schema(description = "팀 ID", example = "12")
        private Long teamId;
        @Schema(description = "유저 ID", example = "101")
        private Long userId;
        @Schema(description = "실패 사유", example = "NOT_IN_MY_TEAM")
        private String reason; // NOT_IN_MY_TEAM, NOT_TEAM_MEMBER, COOLDOWN_24H
    }
}
