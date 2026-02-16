package com.gdg.unimatebackend.calendar.controller;

import com.gdg.unimatebackend.calendar.dto.CalendarDayRequest;
import com.gdg.unimatebackend.calendar.dto.CalendarDayResponse;
import com.gdg.unimatebackend.calendar.dto.CalendarMonthRequest;
import com.gdg.unimatebackend.calendar.dto.CalendarMonthResponse;
import com.gdg.unimatebackend.calendar.service.CalendarService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/calendar")
@Tag(
        name = "캘린더",
        description = "팀 일정(태스크)과 내 개인 일정을 월/일 단위로 조회하는 캘린더 API"
)
public class CalendarController {

    private final CalendarService calendarService;

    @Operation(
            summary = "월 단위 일정 개수 조회",
            description = """
                    월 단위로 날짜별 일정 개수(dayCounts)를 반환합니다.

                    - 팀 일정(태스크)과 내 개인 일정(옵션 includeMyPersonal=true인 경우만)을 합산합니다.
                    - teamIds 미전달 시, 내가 속한 모든 팀 기준으로 집계합니다.
                    - 캘린더에서는 다른 팀원의 개인 일정은 조회하지 않습니다.
                    """,
            security = @SecurityRequirement(name = "bearerAuth")
    )
    @GetMapping("/month")
    public ResponseEntity<CalendarMonthResponse> getMonth(
            Authentication authentication,

            @Parameter(description = "조회 월 (YYYY-MM)", example = "2026-02")
            @RequestParam String month,

            @Parameter(description = "조회할 팀 ID 목록 (미전달 시 전체 팀 기준)", example = "1")
            @RequestParam(required = false) List<Long> teamIds,

            @Parameter(description = "내 개인 일정 포함 여부", example = "true")
            @RequestParam boolean includeMyPersonal
    ) {
        Long userId = (Long) authentication.getPrincipal();

        CalendarMonthRequest request = CalendarMonthRequest.builder()
                .month(month)
                .teamIds(teamIds)
                .includeMyPersonal(includeMyPersonal)
                .build();

        return ResponseEntity.ok(calendarService.getMonth(userId, request));
    }

    @Operation(
            summary = "특정 날짜 일정 목록 조회",
            description = """
                    특정 날짜의 팀 일정 및 내 개인 일정 목록을 반환합니다.

                    - teamSchedules: 팀별로 그룹핑된 팀 일정(태스크) 목록
                    - personalSchedules: 내 개인 일정 목록(includeMyPersonal=true인 경우만)
                    - includeMyPersonal=false 인 경우 내 개인 일정은 제외됩니다.
                    - 캘린더에서는 다른 팀원의 개인 일정은 조회하지 않습니다.
                    """,
            security = @SecurityRequirement(name = "bearerAuth")
    )
    @GetMapping("/day")
    public ResponseEntity<CalendarDayResponse> getDay(
            Authentication authentication,

            @Parameter(description = "조회 날짜 (YYYY-MM-DD)", example = "2026-02-11")
            @RequestParam String date,

            @Parameter(description = "조회할 팀 ID 목록 (미전달 시 전체 팀 기준)", example = "1")
            @RequestParam(required = false) List<Long> teamIds,

            @Parameter(description = "내 개인 일정 포함 여부", example = "true")
            @RequestParam boolean includeMyPersonal
    ) {
        Long userId = (Long) authentication.getPrincipal();

        CalendarDayRequest request = CalendarDayRequest.builder()
                .date(date)
                .teamIds(teamIds)
                .includeMyPersonal(includeMyPersonal)
                .build();

        return ResponseEntity.ok(calendarService.getDay(userId, request));
    }
}