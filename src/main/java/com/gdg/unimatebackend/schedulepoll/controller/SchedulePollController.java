package com.gdg.unimatebackend.schedulepoll.controller;

import com.gdg.unimatebackend.schedulepoll.dto.*;
import com.gdg.unimatebackend.schedulepoll.service.SchedulePollQueryService;
import com.gdg.unimatebackend.schedulepoll.service.SchedulePollService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/schedule-polls")
@Tag(
        name = "모이기",
        description = "팀 회의 시간을 조율하기 위한 모이기(시간 투표) API"
)
public class SchedulePollController {

    private final SchedulePollService schedulePollService;
    private final SchedulePollQueryService schedulePollQueryService;

    @Operation(
            summary = "모이기 생성",
            description = """
                    팀 회의 시간을 조율하기 위한 모이기를 생성합니다.
                    
                    - 팀 멤버만 생성 가능합니다.
                    - 날짜 목록 + 하루 시간 범위를 기준으로 시간 슬롯이 생성됩니다.
                    - 아직 투표 전 상태이며 status=OPEN 으로 시작합니다.
                    """,
            security = @SecurityRequirement(name = "bearerAuth")
    )
    @PostMapping
    public ResponseEntity<SchedulePollCreateResponse> create(
            Authentication authentication,
            @Valid @RequestBody SchedulePollCreateRequest request
    ) {
        Long userId = (Long) authentication.getPrincipal();
        return ResponseEntity.ok(schedulePollService.create(userId, request));
    }

    @Operation(
            summary = "모이기 상세 조회",
            description = """
                    모이기 상세 정보를 조회합니다.
                    
                    - 팀 멤버만 조회 가능합니다.
                    - 멤버 목록, 각 멤버의 투표 현황, 내 선택 시간(mySlots)을 포함합니다.
                    - 모든 팀원이 투표한 경우, 모두 가능한 가장 빠른 시간이 autoFixedSlotId로 계산됩니다.
                    """,
            security = @SecurityRequirement(name = "bearerAuth")
    )
    @GetMapping("/{pollId}")
    public ResponseEntity<SchedulePollDetailResponse> getDetail(
            Authentication authentication,
            @Parameter(description = "모이기 ID", example = "1")
            @PathVariable Long pollId
    ) {
        Long userId = (Long) authentication.getPrincipal();
        return ResponseEntity.ok(schedulePollQueryService.getDetail(userId, pollId));
    }

    @Operation(
            summary = "내 가능 시간 선택/수정",
            description = """
                    내가 가능한 시간 슬롯을 선택하거나 수정합니다.
                    
                    - 기존 투표가 있으면 덮어씁니다 (PUT, 멱등).
                    - 모이기가 확정(locked=true)된 이후에는 수정할 수 없습니다.
                    - 모든 팀원이 투표 완료 시 자동으로 auto-fixed 시간이 계산됩니다.
                    """,
            security = @SecurityRequirement(name = "bearerAuth")
    )
    @PutMapping("/{pollId}/votes/me")
    public ResponseEntity<Void> upsertMyVote(
            Authentication authentication,
            @Parameter(description = "모이기 ID", example = "1")
            @PathVariable Long pollId,
            @Valid @RequestBody SchedulePollVoteUpsertRequest request
    ) {
        Long userId = (Long) authentication.getPrincipal();
        schedulePollService.upsertMyVote(userId, pollId, request);
        return ResponseEntity.ok().build();
    }

    @Operation(
            summary = "모이기 시간 수동 확정",
            description = """
                    모이기 시간을 팀장이 수동으로 확정합니다.
                    
                    - 팀장만 가능합니다.
                    - autoFixedSlotId가 있어도 다른 시간으로 변경할 수 있습니다.
                    - 확정 후에는 locked=true 상태가 되며, 투표 수정이 불가능합니다.
                    """,
            security = @SecurityRequirement(name = "bearerAuth")
    )
    @PatchMapping("/{pollId}/fix")
    public ResponseEntity<Void> fix(
            Authentication authentication,
            @Parameter(description = "모이기 ID", example = "1")
            @PathVariable Long pollId,
            @Valid @RequestBody SchedulePollFixRequest request
    ) {
        Long userId = (Long) authentication.getPrincipal();
        schedulePollService.fix(userId, pollId, request);
        return ResponseEntity.ok().build();
    }

    @Operation(
            summary = "모이기 기본 정보 수정",
            description = """
                    모이기의 제목/메모/알림 설정을 수정합니다.
                    
                    - 팀 멤버만 수정 가능합니다.
                    - 시간 슬롯 자체는 변경되지 않습니다.
                    """,
            security = @SecurityRequirement(name = "bearerAuth")
    )
    @PutMapping("/{pollId}")
    public ResponseEntity<Void> updateMeta(
            Authentication authentication,
            @Parameter(description = "모이기 ID", example = "1")
            @PathVariable Long pollId,
            @Valid @RequestBody SchedulePollMetaUpdateRequest request
    ) {
        Long userId = (Long) authentication.getPrincipal();
        schedulePollService.updateMeta(userId, pollId, request);
        return ResponseEntity.ok().build();
    }

    @Operation(
            summary = "모이기 삭제",
            description = """
                    모이기를 삭제합니다.
                    
                    - 팀 멤버만 삭제 가능합니다.
                    - 삭제 시 투표 정보도 함께 삭제됩니다.
                    """,
            security = @SecurityRequirement(name = "bearerAuth")
    )
    @DeleteMapping("/{pollId}")
    public ResponseEntity<Void> delete(
            Authentication authentication,
            @Parameter(description = "모이기 ID", example = "1")
            @PathVariable Long pollId
    ) {
        Long userId = (Long) authentication.getPrincipal();
        schedulePollService.delete(userId, pollId);
        return ResponseEntity.noContent().build();
    }
}