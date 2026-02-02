package com.gdg.unimatebackend.app.schedule.controller;

import com.gdg.unimatebackend.app.schedule.dto.*;
import com.gdg.unimatebackend.app.schedule.service.MyScheduleService;
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
@RequestMapping("/api/teams/{teamId}/my-schedules")
@Tag(
        name = "개인일정",
        description = """
                팀 내부에서 사용하는 개인 일정 API입니다.

                📌 설계 규칙
                - 모든 일정은 시간 기반(startAt / endAt)으로 관리됩니다.
                - 하루 종일 일정은 startAt=00:00, endAt=다음날 00:00 형태로 표현합니다.
                - '지금 기준 바쁨 여부'는 startAt <= now < endAt 조건으로 판단합니다.
                """
)
public class MyScheduleController {

    private final MyScheduleService myScheduleService;

    @Operation(
            summary = "개인 일정 생성",
            description = """
                    로그인한 사용자의 팀 내부 개인 일정을 생성합니다.

                    📌 규칙
                    - 모든 일정은 startAt / endAt 시간을 반드시 포함해야 합니다.
                    - 하루 종일 일정의 경우:
                      startAt = YYYY-MM-DDT00:00,
                      endAt = (다음날)T00:00 으로 전달해야 합니다.
                    - isPrivate=true 인 경우, 향후 팀 공유 화면에서
                      제목/메모가 비공개 처리될 수 있습니다.

                    📌 사용처
                    - 개인 일정 관리
                    - 현재 시각 기준 Busy/Idle 판단
                    - 캘린더 마킹
                    """,
            security = @SecurityRequirement(name = "bearerAuth")
    )
    @PostMapping
    public ResponseEntity<MyScheduleResponse> create(
            Authentication authentication,
            @Parameter(description = "팀 ID", example = "1")
            @PathVariable Long teamId,
            @Valid @RequestBody MyScheduleCreateRequest request
    ) {
        Long userId = (Long) authentication.getPrincipal();
        return ResponseEntity.ok(myScheduleService.create(userId, teamId, request));
    }

    @Operation(
            summary = "월/기간 단위 개인 일정 조회 (캘린더 마킹용)",
            description = """
                    로그인한 사용자의 개인 일정 중,
                    특정 기간 내 일정이 존재하는 날짜 목록을 반환합니다.

                    📌 특징
                    - 날짜 단위로만 반환합니다 (시간 정보 없음)
                    - 하루에 일정이 하나라도 존재하면 해당 날짜가 포함됩니다.

                    📌 사용처
                    - 캘린더 월뷰/주뷰 마킹
                    - 일정 존재 여부 표시

                    📌 파라미터
                    - from/to는 YYYY-MM-DD 형식의 날짜입니다.
                    """,
            security = @SecurityRequirement(name = "bearerAuth")
    )
    @GetMapping
    public ResponseEntity<MyScheduleMarkingResponse> getMarkedDates(
            Authentication authentication,
            @Parameter(description = "팀 ID", example = "1")
            @PathVariable Long teamId,
            @Parameter(description = "조회 시작 날짜 (YYYY-MM-DD)", example = "2026-02-01")
            @RequestParam LocalDate from,
            @Parameter(description = "조회 종료 날짜 (YYYY-MM-DD)", example = "2026-02-28")
            @RequestParam LocalDate to
    ) {
        Long userId = (Long) authentication.getPrincipal();
        return ResponseEntity.ok(myScheduleService.getMarkedDates(userId, teamId, from, to));
    }

    @Operation(
            summary = "개인 일정 수정 (본인만)",
            description = """
                    본인이 생성한 개인 일정만 수정할 수 있습니다.

                    📌 규칙
                    - startAt / endAt은 항상 시간 범위를 만족해야 합니다.
                    - endAt은 startAt 이후여야 합니다.

                    📌 사용처
                    - 개인 일정 시간/내용 수정
                    """,
            security = @SecurityRequirement(name = "bearerAuth")
    )
    @PutMapping("/{scheduleId}")
    public ResponseEntity<MyScheduleResponse> update(
            Authentication authentication,
            @Parameter(description = "팀 ID", example = "1")
            @PathVariable Long teamId,
            @Parameter(description = "개인 일정 ID", example = "10")
            @PathVariable Long scheduleId,
            @Valid @RequestBody MyScheduleUpdateRequest request
    ) {
        Long userId = (Long) authentication.getPrincipal();
        return ResponseEntity.ok(myScheduleService.update(userId, teamId, scheduleId, request));
    }

    @Operation(
            summary = "개인 일정 삭제 (본인만)",
            description = """
                    본인이 생성한 개인 일정만 삭제할 수 있습니다.

                    📌 주의
                    - 삭제된 일정은 복구할 수 없습니다.
                    """,
            security = @SecurityRequirement(name = "bearerAuth")
    )
    @DeleteMapping("/{scheduleId}")
    public ResponseEntity<Void> delete(
            Authentication authentication,
            @Parameter(description = "팀 ID", example = "1")
            @PathVariable Long teamId,
            @Parameter(description = "개인 일정 ID", example = "10")
            @PathVariable Long scheduleId
    ) {
        Long userId = (Long) authentication.getPrincipal();
        myScheduleService.delete(userId, teamId, scheduleId);
        return ResponseEntity.noContent().build();
    }

    @Operation(
            summary = "현재 시각 기준 개인 일정 상태 (Busy/Idle)",
            description = """
                    로그인한 사용자의 '현재 시각 기준' 개인 일정 상태를 조회합니다.

                    📌 Busy 판단 기준
                    - startAt <= 현재 시각 < endAt 인 일정이 하나라도 존재하면 Busy

                    📌 응답
                    - isBusy: 현재 바쁜 상태인지 여부
                    - schedules: 현재 시각과 겹치는 일정 목록

                    📌 사용처
                    - 팀원 상태 표시 (지금 바쁨/한가함)
                    - 찌르기/알림 가능 여부 판단
                    """,
            security = @SecurityRequirement(name = "bearerAuth")
    )
    @GetMapping("/now")
    public ResponseEntity<MyScheduleNowResponse> getNowStatus(
            Authentication authentication,
            @Parameter(description = "팀 ID", example = "1")
            @PathVariable Long teamId
    ) {
        Long userId = (Long) authentication.getPrincipal();
        return ResponseEntity.ok(myScheduleService.getNowStatus(userId, teamId));
    }

    @Operation(
            summary = "특정 날짜의 개인 일정 목록 조회",
            description = """
                    로그인한 사용자의 특정 날짜에 해당하는 개인 일정 목록을 조회합니다.

                    📌 특징
                    - 시간 범위(startAt/endAt)를 그대로 반환합니다.
                    - 하루 종일 일정도 00:00~다음날 00:00 형태로 포함됩니다.

                    📌 사용처
                    - 일별 일정 상세 화면
                    - 디버깅 및 QA 확인
                    """,
            security = @SecurityRequirement(name = "bearerAuth")
    )
    @GetMapping("/day")
    public ResponseEntity<List<MyScheduleResponse>> getDaySchedules(
            Authentication authentication,
            @Parameter(description = "팀 ID", example = "1")
            @PathVariable Long teamId,
            @Parameter(description = "조회 날짜 (YYYY-MM-DD)", example = "2026-02-10")
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date
    ) {
        Long userId = (Long) authentication.getPrincipal();
        return ResponseEntity.ok(
                myScheduleService.getDaySchedules(userId, teamId, date)
        );
    }
}
