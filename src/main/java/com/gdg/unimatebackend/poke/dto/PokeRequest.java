package com.gdg.unimatebackend.poke.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.List;

@Getter
@NoArgsConstructor
@Schema(description = "찌르기 전송 요청")
public class PokeRequest {

    @Schema(description = "선택한 찌르기 문구 ID", example = "3")
    private Long messageId;
    @Schema(description = "찌르기 대상 목록(최대 50)")
    private List<Target> targets;

    @Getter
    @NoArgsConstructor
    @Schema(description = "찌르기 대상 항목")
    public static class Target {
        @Schema(description = "팀 ID", example = "12")
        private Long teamId;
        @Schema(description = "대상 유저 ID", example = "101")
        private Long userId;
    }
}
