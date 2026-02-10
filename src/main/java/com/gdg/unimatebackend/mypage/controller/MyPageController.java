package com.gdg.unimatebackend.mypage.controller;

import com.gdg.unimatebackend.mypage.dto.MyPageSummaryResponse;
import com.gdg.unimatebackend.mypage.service.MyPageService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/mypage")
@Tag(name = "마이페이지", description = "마이페이지 요약 조회 API")
public class MyPageController {

    private final MyPageService myPageService;

    @Operation(
            summary = "마이페이지 요약 조회",
            description = """
                    마이페이지 상단 프로필 + 참여중/완료 팀플 리스트를 조회합니다.
                    - 참여중/완료 구분은 팀의 isCompleted(endAt < today) 기준입니다.
                    """,
            security = @SecurityRequirement(name = "bearerAuth")
    )
    @GetMapping
    public ResponseEntity<MyPageSummaryResponse> getSummary(Authentication authentication) {
        Long userId = (Long) authentication.getPrincipal();
        return ResponseEntity.ok(myPageService.getMyPageSummary(userId));
    }
}
