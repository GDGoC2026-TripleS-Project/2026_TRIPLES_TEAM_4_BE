package com.gdg.unimatebackend.team.controller;

import com.gdg.unimatebackend.team.dto.*;
import com.gdg.unimatebackend.team.service.TeamImageService;
import com.gdg.unimatebackend.team.service.TeamService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/teams")
@Tag(name = "팀스페이스", description = "팀스페이스 생성/조회/수정/삭제/탈퇴/초대/참여 API")
public class TeamController {

    private final TeamService teamService;
    private final TeamImageService teamImageService;

    @Operation(
            summary = "팀 생성",
            description = """
                    팀스페이스를 생성합니다.
                    - 생성자는 자동으로 팀장(LEADER)으로 가입됩니다.
                    """,
            security = @SecurityRequirement(name = "bearerAuth")
    )
    @PostMapping
    public ResponseEntity<TeamResponse> create(
            Authentication authentication,
            @Valid @RequestBody TeamCreateRequest request
    ) {
        Long userId = (Long) authentication.getPrincipal();
        return ResponseEntity.ok(teamService.createTeam(userId, request));
    }

    @Operation(
            summary = "내 팀 목록 조회",
            description = """
                    내가 속한 팀 목록을 조회합니다.
                    - 각 팀별로 myRole(내 역할), memberCount(팀원 수)를 함께 반환합니다.
                    """,
            security = @SecurityRequirement(name = "bearerAuth")
    )
    @GetMapping
    public ResponseEntity<List<TeamSummaryResponse>> getMyTeams(Authentication authentication) {
        Long userId = (Long) authentication.getPrincipal();
        return ResponseEntity.ok(teamService.getMyTeams(userId));
    }

    @Operation(
            summary = "팀 상세 조회",
            description = """
                    팀 상세 정보를 조회합니다.
                    - 팀 정보(team) + 팀원 목록(members)
                    - myRole(내 역할), memberCount(팀원 수) 포함
                    - 팀 멤버만 접근 가능합니다.
                    """,
            security = @SecurityRequirement(name = "bearerAuth")
    )
    @GetMapping("/{teamId}")
    public ResponseEntity<TeamDetailResponse> getDetail(
            Authentication authentication,
            @Parameter(description = "팀 ID", example = "1")
            @PathVariable Long teamId
    ) {
        Long userId = (Long) authentication.getPrincipal();
        return ResponseEntity.ok(teamService.getTeamDetail(userId, teamId));
    }

    @Operation(
            summary = "팀 정보 수정",
            description = """
                    팀 정보를 수정합니다. (팀장만)
                    - name/description/color 중 전달된 값만 반영됩니다.
                    """,
            security = @SecurityRequirement(name = "bearerAuth")
    )
    @PutMapping("/{teamId}")
    public ResponseEntity<TeamResponse> update(
            Authentication authentication,
            @Parameter(description = "팀 ID", example = "1")
            @PathVariable Long teamId,
            @Valid @RequestBody TeamUpdateRequest request
    ) {
        Long userId = (Long) authentication.getPrincipal();
        return ResponseEntity.ok(teamService.updateTeam(userId, teamId, request));
    }

    @Operation(
            summary = "팀 이미지 업로드",
            description = """
                    팀 이미지를 업로드합니다. (팀장만)
                    - multipart/form-data 형식의 이미지 파일을 업로드합니다.
                    - 업로드된 이미지는 AWS S3에 저장됩니다.
                    - 기존 팀 이미지가 있으면 이전 이미지는 S3에서 삭제됩니다.
                    """,
            security = @SecurityRequirement(name = "bearerAuth")
    )
    @PostMapping(
            value = "/{teamId}/image",
            consumes = MediaType.MULTIPART_FORM_DATA_VALUE
    )
    public ResponseEntity<java.util.Map<String, String>> uploadTeamImage(
            Authentication authentication,
            @PathVariable Long teamId,
            @RequestPart("file") MultipartFile file
    ) {
        Long userId = (Long) authentication.getPrincipal();
        String imageUrl = teamImageService.uploadAndReplaceTeamImage(userId, teamId, file);
        return ResponseEntity.ok(java.util.Map.of("imageUrl", imageUrl));
    }

    @Operation(
            summary = "팀 삭제",
            description = """
                    팀을 삭제합니다. (팀장만)
                    - 팀 삭제 시 팀원 데이터도 함께 정리됩니다.
                    """,
            security = @SecurityRequirement(name = "bearerAuth")
    )
    @DeleteMapping("/{teamId}")
    public ResponseEntity<Void> delete(
            Authentication authentication,
            @Parameter(description = "팀 ID", example = "1")
            @PathVariable Long teamId
    ) {
        Long userId = (Long) authentication.getPrincipal();
        teamService.deleteTeam(userId, teamId);
        return ResponseEntity.noContent().build();
    }

    @Operation(
            summary = "팀 탈퇴",
            description = """
                    팀에서 탈퇴합니다. (팀원만)
                    - 팀장(LEADER)은 탈퇴할 수 없습니다. (정책: 팀 삭제 사용)
                    """,
            security = @SecurityRequirement(name = "bearerAuth")
    )
    @DeleteMapping("/{teamId}/leave")
    public ResponseEntity<Void> leave(
            Authentication authentication,
            @Parameter(description = "팀 ID", example = "1")
            @PathVariable Long teamId
    ) {
        Long userId = (Long) authentication.getPrincipal();
        teamService.leaveTeam(userId, teamId);
        return ResponseEntity.noContent().build();
    }

    @Operation(
            summary = "팀원 목록 조회",
            description = """
                    팀원 목록을 조회합니다.
                    - 팀 멤버만 조회 가능합니다.
                    """,
            security = @SecurityRequirement(name = "bearerAuth")
    )
    @GetMapping("/{teamId}/members")
    public ResponseEntity<List<TeamMemberResponse>> members(
            Authentication authentication,
            @Parameter(description = "팀 ID", example = "1")
            @PathVariable Long teamId
    ) {
        Long userId = (Long) authentication.getPrincipal();
        return ResponseEntity.ok(teamService.getTeamMembers(userId, teamId));
    }

    @Operation(
            summary = "초대코드 발급/재발급",
            description = """
                    팀 초대코드를 발급(또는 재발급)합니다. (팀장만)
                    - 숫자 6자리
                    - 발급 시점부터 10분 뒤 만료
                    - 재발급 시 기존 코드는 즉시 교체됩니다.
                    """,
            security = @SecurityRequirement(name = "bearerAuth")
    )
    @PostMapping("/{teamId}/invite-code")
    public ResponseEntity<TeamInviteCodeResponse> issueInviteCode(
            Authentication authentication,
            @Parameter(description = "팀 ID", example = "1")
            @PathVariable Long teamId
    ) {
        Long userId = (Long) authentication.getPrincipal();
        return ResponseEntity.ok(teamService.issueInviteCode(userId, teamId));
    }

    @Operation(
            summary = "초대코드로 팀 참여",
            description = """
                    초대코드로 팀에 참여합니다.
                    - 숫자 6자리 inviteCode 필요
                    - 만료(10분)된 코드는 참여 불가
                    - 이미 멤버면 성공 처리(멱등)
                    - 참여 성공 시 (선택) '새 팀원 입장' 알림을 팀원들에게 발송할 수 있습니다.
                    
                    커스텀 에러 코드 예시:
                    - INVITE_CODE_INVALID: 유효하지 않은 코드
                    - INVITE_CODE_EXPIRED: 만료된 코드
                    """,
            security = @SecurityRequirement(name = "bearerAuth")
    )
    @PostMapping("/join")
    public ResponseEntity<TeamJoinResponse> join(
            Authentication authentication,
            @Valid @RequestBody TeamJoinRequest request
    ) {
        Long userId = (Long) authentication.getPrincipal();
        return ResponseEntity.ok(teamService.joinByInviteCode(userId, request));
    }

    @Operation(
            summary = "사용 가능한 팀 색상",
            description = """
                    초대코드로 팀에 참여합니다.
                    이미 있는 팀의 색상의 컬러는 사용 불가합니다
                    """,
            security = @SecurityRequirement(name = "bearerAuth")
    )
    @GetMapping("/colors/available")
    public ResponseEntity<List<TeamColorResponse>> getAvailableColors(Authentication authentication) {
        Long userId = (Long) authentication.getPrincipal();
        return ResponseEntity.ok(teamService.getAvailableColors(userId));
    }

    @Operation(
            summary = "초대코드 조회",
            description = """
                팀 초대코드를 조회합니다.
                - 팀 멤버만 조회 가능합니다.
                - 초대코드가 없거나(발급 전) 만료된 경우 에러를 반환합니다.
                """,
            security = @SecurityRequirement(name = "bearerAuth")
    )
    @GetMapping("/{teamId}/invite")
    public ResponseEntity<TeamInviteCodeResponse> getInviteCode(
            Authentication authentication,
            @Parameter(description = "팀 ID", example = "1")
            @PathVariable Long teamId
    ) {
        Long userId = (Long) authentication.getPrincipal();
        return ResponseEntity.ok(teamService.getInviteCode(userId, teamId));
    }
}
