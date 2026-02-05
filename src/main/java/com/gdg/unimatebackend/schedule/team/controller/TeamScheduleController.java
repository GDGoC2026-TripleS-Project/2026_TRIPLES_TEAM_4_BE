package com.gdg.unimatebackend.schedule.team.controller;

import com.gdg.unimatebackend.schedule.team.dto.*;
import com.gdg.unimatebackend.schedule.team.service.TeamScheduleService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/teams/{teamId}/team-schedules")
@Tag(
        name = "팀일정",
        description = """
                팀에 공유되는 팀 일정(태스크) API입니다.

                📌 설계 규칙
                - 모든 일정은 시간 기반(startAt / endAt)으로 관리됩니다.
                - 하루 종일 일정은 startAt=00:00, endAt=다음날 00:00 형태로 표현합니다.
                - 기간 겹침(overlap) 규칙: startAt < rangeEnd AND endAt > rangeStart
                """
)
public class TeamScheduleController {

    private final TeamScheduleService teamScheduleService;

    @Operation(
            summary = "팀 일정 생성",
            description = """
                    팀에 공유되는 팀 일정을 생성합니다.

                    ✅ 권한
                    - 팀 멤버 누구나 생성 가능

                    ✅ 규칙
                    - startAt/endAt 필수
                    - endAt은 startAt 이후여야 함
                    """,
            security = @SecurityRequirement(name = "bearerAuth")
    )
    @PostMapping
    public ResponseEntity<TeamScheduleResponse> create(
            Authentication authentication,
            @Parameter(description = "팀 ID", example = "1")
            @PathVariable Long teamId,
            @Valid @RequestBody TeamScheduleCreateRequest request
    ) {
        Long userId = (Long) authentication.getPrincipal();
        return ResponseEntity.ok(teamScheduleService.create(userId, teamId, request));
    }

    @Operation(
            summary = "팀 일정 수정",
            description = """
                    팀 일정을 수정합니다.

                    ✅ 권한
                    - 작성자(createdBy) 또는 팀장(LEADER)만 가능
                    """,
            security = @SecurityRequirement(name = "bearerAuth")
    )
    @PutMapping("/{scheduleId}")
    public ResponseEntity<TeamScheduleResponse> update(
            Authentication authentication,
            @Parameter(description = "팀 ID", example = "1")
            @PathVariable Long teamId,
            @Parameter(description = "팀 일정 ID", example = "10")
            @PathVariable Long scheduleId,
            @Valid @RequestBody TeamScheduleUpdateRequest request
    ) {
        Long userId = (Long) authentication.getPrincipal();
        return ResponseEntity.ok(teamScheduleService.update(userId, teamId, scheduleId, request));
    }

    @Operation(
            summary = "팀 일정 삭제",
            description = """
                    팀 일정을 삭제합니다.

                    ✅ 권한
                    - 작성자(createdBy) 또는 팀장(LEADER)만 가능
                    """,
            security = @SecurityRequirement(name = "bearerAuth")
    )
    @DeleteMapping("/{scheduleId}")
    public ResponseEntity<Void> delete(
            Authentication authentication,
            @Parameter(description = "팀 ID", example = "1")
            @PathVariable Long teamId,
            @Parameter(description = "팀 일정 ID", example = "10")
            @PathVariable Long scheduleId
    ) {
        Long userId = (Long) authentication.getPrincipal();
        teamScheduleService.delete(userId, teamId, scheduleId);
        return ResponseEntity.noContent().build();
    }

    @Operation(
            summary = "기간 단위 팀 일정 조회",
            description = """
                    팀 일정만 기간 단위로 조회합니다.
                    캘린더 마킹/리스트에 사용합니다.

                    ✅ 파라미터
                    - from/to: YYYY-MM-DD
                    """,
            security = @SecurityRequirement(name = "bearerAuth")
    )
    @GetMapping
    public ResponseEntity<List<TeamScheduleResponse>> getByRange(
            Authentication authentication,
            @Parameter(description = "팀 ID", example = "1")
            @PathVariable Long teamId,
            @Parameter(description = "조회 시작 날짜 (YYYY-MM-DD)", example = "2026-02-01")
            @RequestParam LocalDate from,
            @Parameter(description = "조회 종료 날짜 (YYYY-MM-DD)", example = "2026-02-28")
            @RequestParam LocalDate to
    ) {
        Long userId = (Long) authentication.getPrincipal();
        return ResponseEntity.ok(teamScheduleService.getByRange(userId, teamId, from, to));
    }

    @Operation(
            summary = "특정 날짜 팀 일정 조회",
            description = """
                    특정 날짜에 해당하는 팀 일정 목록을 반환합니다.
                    (일별 상세 화면용)
                    """,
            security = @SecurityRequirement(name = "bearerAuth")
    )
    @GetMapping("/day")
    public ResponseEntity<List<TeamScheduleResponse>> getByDay(
            Authentication authentication,
            @Parameter(description = "팀 ID", example = "1")
            @PathVariable Long teamId,
            @Parameter(description = "조회 날짜 (YYYY-MM-DD)", example = "2026-02-10")
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date
    ) {
        Long userId = (Long) authentication.getPrincipal();
        return ResponseEntity.ok(teamScheduleService.getByDay(userId, teamId, date));
    }
}
