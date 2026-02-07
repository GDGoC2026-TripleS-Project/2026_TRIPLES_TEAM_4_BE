package com.gdg.unimatebackend.schedulepoll.controller;

import com.gdg.unimatebackend.schedulepoll.dto.TeamFixedSchedulePollSummaryResponse;
import com.gdg.unimatebackend.schedulepoll.service.SchedulePollQueryService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

@RequiredArgsConstructor
@RestController
@RequestMapping("/api/teams/{teamId}/schedule-polls")
@Tag(name = "모이기", description = "팀 회의 시간을 조율하기 위한 모이기(시간 투표) API")
public class TeamSchedulePollController {

    private final SchedulePollQueryService schedulePollQueryService;

    @Operation(
            summary = "팀 확정 모이기 목록 조회(기간)",
            description = """
                    특정 기간 내에 확정된 모이기 목록을 조회합니다.
                    - 팀 멤버만 조회 가능합니다.
                    - 캘린더 '전체' 탭 / 월 마킹용
                    - 확정된 회의만 반환합니다(AUTO_FIXED / MANUALLY_FIXED)
                    """,
            security = @SecurityRequirement(name = "bearerAuth")
    )
    @GetMapping("/fixed")
    public ResponseEntity<List<TeamFixedSchedulePollSummaryResponse>> fixedRange(
            Authentication authentication,
            @Parameter(description = "팀 ID", example = "1")
            @PathVariable Long teamId,
            @Parameter(description = "조회 시작 날짜", example = "2026-02-01")
            @RequestParam LocalDate from,
            @Parameter(description = "조회 종료 날짜", example = "2026-02-28")
            @RequestParam LocalDate to
    ) {
        Long userId = (Long) authentication.getPrincipal();
        return ResponseEntity.ok(schedulePollQueryService.getTeamFixedRange(userId, teamId, from, to));
    }

    @Operation(
            summary = "팀 확정 모이기 목록 조회(하루)",
            description = """
                    특정 날짜의 확정된 모이기 목록을 조회합니다.
                    - 팀 멤버만 조회 가능합니다.
                    - 캘린더에서 날짜 클릭 시 '전체' 탭에 보여줄 데이터
                    """,
            security = @SecurityRequirement(name = "bearerAuth")
    )
    @GetMapping("/fixed/day")
    public ResponseEntity<List<TeamFixedSchedulePollSummaryResponse>> fixedDay(
            Authentication authentication,
            @Parameter(description = "팀 ID", example = "1")
            @PathVariable Long teamId,
            @Parameter(description = "조회 날짜", example = "2026-02-23")
            @RequestParam LocalDate date
    ) {
        Long userId = (Long) authentication.getPrincipal();
        return ResponseEntity.ok(schedulePollQueryService.getTeamFixedDay(userId, teamId, date));
    }
}
