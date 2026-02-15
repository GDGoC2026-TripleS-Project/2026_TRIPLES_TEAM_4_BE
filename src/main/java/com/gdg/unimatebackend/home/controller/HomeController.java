package com.gdg.unimatebackend.home.controller;

import com.gdg.unimatebackend.home.dto.HomeSummaryResponse;
import com.gdg.unimatebackend.home.service.HomeService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import java.util.Arrays;
import java.util.List;

import java.time.LocalDate;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/home")
@Tag(name = "홈", description = "홈 화면(주간 캘린더/오늘의 일/팀스페이스/알림 뱃지) API")
public class HomeController {

    private final HomeService homeService;

    @Operation(
            summary = "홈 화면 요약 조회",
            description = """
                    홈 화면에 필요한 데이터를 한 번에 조회합니다.
                    - 주간 캘린더(일~토): 날짜별 일정 개수 포함
                    - 오늘의 일: 팀 일정 + 내 개인 일정
                    - 나의 팀 스페이스 목록
                    - 알림 뱃지(현재는 FCM 토큰만 존재하여 unreadCount는 0으로 반환)
                    
                    📌 주간 캘린더 일정 개수(count) 규칙:
                    - 팀 일정: 카운트 포함
                    - 내 개인 일정: includeMyPersonal=true 일 때 카운트 포함
                    - 타인 개인 일정: 홈 화면에서는 카운트에 포함하지 않음
                    """,
            security = @SecurityRequirement(name = "bearerAuth")
    )
    @GetMapping
    public ResponseEntity<HomeSummaryResponse> getHomeSummary(
            Authentication authentication,
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
            LocalDate date,

            @RequestParam(required = false)
            Long[] teamIds,

            @RequestParam(required = false, defaultValue = "true")
            boolean includeMyPersonal
    ) {
        Long userId = (Long) authentication.getPrincipal();

        List<Long> teamIdList = (teamIds == null) ? null : Arrays.asList(teamIds);

        return ResponseEntity.ok(homeService.getHomeSummary(userId, date, teamIdList, includeMyPersonal));
    }
}