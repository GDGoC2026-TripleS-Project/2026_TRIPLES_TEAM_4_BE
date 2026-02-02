package com.gdg.unimatebackend.app.schedule.team.controller;

import com.gdg.unimatebackend.app.schedule.team.dto.TeamMemberBusyNowResponse;
import com.gdg.unimatebackend.app.schedule.team.service.TeamMemberBusyNowService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/teams/{teamId}/members")
@Tag(
        name = "팀원 상태",
        description = "팀원 Busy/Idle 상태 조회 API"
)
public class TeamMemberBusyNowController {

    private final TeamMemberBusyNowService teamMemberBusyNowService;

    @Operation(
            summary = "팀원 전체 '지금 기준' Busy 조회",
            description = """
                    팀원 리스트에서 "지금 바쁨/한가함" 표시를 위해 사용합니다.

                    ✅ N+1 방지
                    - 개인일정(/my-schedules/now)을 팀원 수만큼 호출하지 않고,
                      서버에서 한 번에 계산하여 내려줍니다.

                    ✅ Busy 기준
                    - startAt <= now < endAt 인 개인일정 존재 여부
                    """,
            security = @SecurityRequirement(name = "bearerAuth")
    )
    @GetMapping("/busy-now")
    public ResponseEntity<TeamMemberBusyNowResponse> getBusyNow(
            Authentication authentication,
            @Parameter(description = "팀 ID", example = "1")
            @PathVariable Long teamId
    ) {
        Long userId = (Long) authentication.getPrincipal();
        return ResponseEntity.ok(teamMemberBusyNowService.getBusyNow(userId, teamId));
    }
}
