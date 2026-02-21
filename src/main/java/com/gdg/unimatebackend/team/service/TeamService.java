package com.gdg.unimatebackend.team.service;

import com.gdg.unimatebackend.notification.service.TeamEndNotificationService;
import com.gdg.unimatebackend.team.dto.*;
import com.gdg.unimatebackend.team.entity.*;
import com.gdg.unimatebackend.team.event.TeamJoinedEvent;
import com.gdg.unimatebackend.team.event.TeamLeftEvent;
import com.gdg.unimatebackend.team.exception.TeamErrorCodes;
import com.gdg.unimatebackend.team.exception.TeamException;
import com.gdg.unimatebackend.team.repository.TeamMemberRepository;
import com.gdg.unimatebackend.team.repository.TeamRepository;
import com.gdg.unimatebackend.user.entity.User;
import com.gdg.unimatebackend.user.repository.UserRepository;
import jakarta.validation.constraints.NotNull;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class TeamService {

    private final TeamRepository teamRepository;
    private final TeamMemberRepository teamMemberRepository;
    private final UserRepository userRepository;
    private final ApplicationEventPublisher eventPublisher;
    private final TeamEndNotificationService teamEndNotificationService;

    private static final SecureRandom RANDOM = new SecureRandom();

    // ===== 팀 생성 =====
    @Transactional
    public TeamResponse createTeam(Long userId, TeamCreateRequest request) {
        validateTeamPeriod(request.getStartAt(), request.getEndAt());

        Team team = Team.builder()
                .name(request.getName())
                .description(request.getDescription())
                .color(request.getColor()) // 팀 대표색 고정
                .ownerUserId(userId)
                .startAt(request.getStartAt())
                .endAt(request.getEndAt())
                .build();

        team = teamRepository.save(team);

        // 생성자는 팀 대표색 그대로 표시
        teamMemberRepository.save(TeamMember.builder()
                .teamId(team.getId())
                .userId(userId)
                .role(TeamRole.LEADER)
                .displayColor(team.getColor())
                .joinedAt(LocalDateTime.now())
                .build());

        return toTeamResponse(team);
    }

    // ===== 팀 목록(요약) =====
    @Transactional(readOnly = true)
    public List<TeamSummaryResponse> getMyTeams(Long userId) {
        List<TeamMember> memberships = teamMemberRepository.findAllByUserIdOrderByJoinedAtDesc(userId);
        if (memberships.isEmpty()) return List.of();

        List<Long> teamIdsOrdered = memberships.stream()
                .map(TeamMember::getTeamId)
                .distinct()
                .toList();

        Map<Long, Team> teamMap = teamRepository.findAllById(teamIdsOrdered).stream()
                .collect(Collectors.toMap(Team::getId, t -> t));

        Map<Long, TeamRole> myRoleMap = memberships.stream()
                .collect(Collectors.toMap(TeamMember::getTeamId, TeamMember::getRole, (a, b) -> a));

        Map<Long, TeamColor> myDisplayColorMap = memberships.stream()
                .collect(Collectors.toMap(TeamMember::getTeamId, TeamMember::getDisplayColor, (a, b) -> a));

        List<TeamSummaryResponse> result = new ArrayList<>();
        for (Long teamId : teamIdsOrdered) {
            Team t = teamMap.get(teamId);
            if (t == null) continue;

            long memberCount = teamMemberRepository.countByTeamId(teamId);
            TeamRole myRole = myRoleMap.get(teamId);
            TeamColor myDisplayColor = myDisplayColorMap.get(teamId);

            result.add(TeamSummaryResponse.builder()
                    .id(t.getId())
                    .name(t.getName())
                    .description(t.getDescription())
                    .color(myDisplayColor)
                    .colorHex(myDisplayColor != null ? myDisplayColor.getHex() : null)
                    .imageUrl(t.getImageUrl())
                    .myRole(myRole)
                    .memberCount(memberCount)
                    .startAt(t.getStartAt())
                    .endAt(t.getEndAt())
                    .isCompleted(isCompleted(t.getEndAt()))
                    .createdAt(t.getCreatedAt())
                    .updatedAt(t.getUpdatedAt())
                    .build());
        }
        return result;
    }

    // ===== 팀 상세 =====
    @Transactional(readOnly = true)
    public TeamDetailResponse getTeamDetail(Long userId, Long teamId) {
        Team team = teamRepository.findById(teamId)
                .orElseThrow(() -> new TeamException(
                        TeamErrorCodes.TEAM_NOT_FOUND, "팀을 찾을 수 없습니다", 404
                ));

        TeamMember me = teamMemberRepository.findByTeamIdAndUserId(teamId, userId)
                .orElseThrow(() -> new TeamException(
                        TeamErrorCodes.NOT_A_MEMBER, "팀 멤버만 접근할 수 있습니다", 403
                ));

        List<TeamMember> members = teamMemberRepository.findAllByTeamIdOrderByJoinedAtAsc(teamId);
        List<TeamMemberResponse> memberResponses = buildMemberResponses(members);

        // 상세에서도 "내 표시색"을 별도 필드로 내려주기
        return TeamDetailResponse.builder()
                .team(toTeamResponse(team))
                .myDisplayColor(me.getDisplayColor())
                .myDisplayColorHex(me.getDisplayColor() != null ? me.getDisplayColor().getHex() : null)
                .members(memberResponses)
                .myRole(me.getRole())
                .memberCount(members.size())
                .build();
    }

    // ===== 팀 정보 수정 (팀장만) =====
    @Transactional
    public TeamResponse updateTeam(Long userId, Long teamId, TeamUpdateRequest request) {
        Team team = teamRepository.findById(teamId)
                .orElseThrow(() -> new TeamException(
                        TeamErrorCodes.TEAM_NOT_FOUND, "팀을 찾을 수 없습니다", 404
                ));

        requireLeader(teamId, userId, team);

        // 정책: color 변경 불가 (name/description만)
        if (request.getStartAt() != null || request.getEndAt() != null) {
            LocalDateTime newStart = (request.getStartAt() != null) ? request.getStartAt() : team.getStartAt();
            LocalDateTime newEnd = (request.getEndAt() != null) ? request.getEndAt() : team.getEndAt();
            validateTeamPeriod(newStart, newEnd);
        }
        team.update(request.getName(), request.getDescription(), request.getStartAt(), request.getEndAt());
        if (request.getImageUrl() != null && !request.getImageUrl().isBlank()) {
            team.updateImage(team.getImageKey(), request.getImageUrl());
        }
        teamEndNotificationService.notifyIfEnded(team, userId);
        return toTeamResponse(team);
    }

    // ===== 팀 색상(선택 가능 목록) =====
    @Transactional(readOnly = true)
    public List<TeamColorResponse> getAvailableColors(Long userId) {
        Set<TeamColor> used = new HashSet<>(teamMemberRepository.findDistinctDisplayColorsByUserId(userId));

        List<TeamColorResponse> result = new ArrayList<>();
        for (TeamColor color : TeamColor.values()) {
            if (!used.contains(color)) {
                result.add(new TeamColorResponse(color.name(), color.getHex()));
            }
        }

        // 전부 사용 중이면 전부 내려주기(선택 불가 UX 방지)
        if (result.isEmpty()) {
            for (TeamColor color : TeamColor.values()) {
                result.add(new TeamColorResponse(color.name(), color.getHex()));
            }
        }
        return result;
    }

    // ===== 팀 삭제 =====
    @Transactional
    public void deleteTeam(Long userId, Long teamId) {
        Team team = teamRepository.findById(teamId)
                .orElseThrow(() -> new TeamException(
                        TeamErrorCodes.TEAM_NOT_FOUND, "팀을 찾을 수 없습니다", 404
                ));

        requireLeader(teamId, userId, team);

        teamMemberRepository.deleteAllByTeamId(teamId);
        teamRepository.delete(team);
    }

    // ===== 팀 탈퇴 =====
    @Transactional
    public void leaveTeam(Long userId, Long teamId) {
        TeamMember me = teamMemberRepository.findByTeamIdAndUserId(teamId, userId)
                .orElseThrow(() -> new TeamException(
                        TeamErrorCodes.NOT_A_MEMBER, "팀 멤버가 아닙니다", 403
                ));

        if (me.getRole() == TeamRole.LEADER) {
            throw new TeamException(
                    TeamErrorCodes.LEADER_CANNOT_LEAVE, "팀장은 탈퇴할 수 없습니다. 팀 삭제를 사용해주세요", 400
            );
        }

        teamMemberRepository.deleteByTeamIdAndUserId(teamId, userId);

        // 탈퇴 성공 시(커밋 이후) 남은 팀원들에게 알림
        eventPublisher.publishEvent(new TeamLeftEvent(teamId, userId));
    }

    // ===== 팀원 목록 조회 =====
    @Transactional(readOnly = true)
    public List<TeamMemberResponse> getTeamMembers(Long userId, Long teamId) {
        if (!teamMemberRepository.existsByTeamIdAndUserId(teamId, userId)) {
            throw new TeamException(TeamErrorCodes.NOT_A_MEMBER, "팀 멤버만 접근할 수 있습니다", 403);
        }

        List<TeamMember> members = teamMemberRepository.findAllByTeamIdOrderByJoinedAtAsc(teamId);
        return buildMemberResponses(members);
    }

    // ===== 초대코드 발급/재발급 (팀장만, 10분) =====
    @Transactional
    public TeamInviteCodeResponse issueInviteCode(Long userId, Long teamId) {
        Team team = teamRepository.findById(teamId)
                .orElseThrow(() -> new TeamException(
                        TeamErrorCodes.TEAM_NOT_FOUND, "팀을 찾을 수 없습니다", 404
                ));

        requireLeader(teamId, userId, team);

        String code = generateUnique6DigitCode();
        LocalDateTime expiresAt = LocalDateTime.now().plusMinutes(10);

        team.issueInviteCode(code, expiresAt);

        return TeamInviteCodeResponse.builder()
                .teamId(teamId)
                .inviteCode(code)
                .expiresAt(expiresAt)
                .build();
    }

    // ===== 초대코드로 팀 참여 =====
    @Transactional
    public TeamJoinResponse joinByInviteCode(Long userId, TeamJoinRequest request) {

        Team team = teamRepository.findByInviteCode(request.getInviteCode())
                .orElseThrow(() -> new TeamException(
                        TeamErrorCodes.INVITE_CODE_INVALID, "유효하지 않은 초대코드입니다", 400
                ));

        if (team.getInviteCodeExpiresAt() == null || team.getInviteCodeExpiresAt().isBefore(LocalDateTime.now())) {
            throw new TeamException(
                    TeamErrorCodes.INVITE_CODE_EXPIRED, "초대코드가 만료되었습니다", 400
            );
        }

        Long teamId = team.getId();

        Optional<TeamMember> existing = teamMemberRepository.findByTeamIdAndUserId(teamId, userId);

        TeamMember me;
        if (existing.isPresent()) {
            me = existing.get();
        } else {
            // 핵심: "내 displayColor" 기준으로 중복이면 랜덤 재배정
            TeamColor assigned = assignDisplayColorForUser(userId, team.getColor());

            me = teamMemberRepository.save(
                    TeamMember.builder()
                            .teamId(teamId)
                            .userId(userId)
                            .role(TeamRole.MEMBER)
                            .displayColor(assigned)
                            .joinedAt(LocalDateTime.now())
                            .build()
            );

        }

        // join 요청이 들어오면(신규/기존 멤버 모두) 알림 이벤트 발행
        eventPublisher.publishEvent(new TeamJoinedEvent(teamId, userId));

        List<TeamMember> members = teamMemberRepository.findAllByTeamIdOrderByJoinedAtAsc(teamId);
        List<TeamMemberResponse> memberResponses = buildMemberResponses(members);

        return TeamJoinResponse.builder()
                .team(toTeamResponse(team))
                .myRole(me.getRole())
                .myDisplayColor(me.getDisplayColor())
                .myDisplayColorHex(me.getDisplayColor() != null ? me.getDisplayColor().getHex() : null)
                .memberCount(members.size())
                .members(memberResponses)
                .build();
    }

    // 중복 제외 랜덤 배정 로직 (요구사항 핵심)
    private TeamColor assignDisplayColorForUser(Long userId, TeamColor teamColor) {
        Set<TeamColor> used = new HashSet<>(teamMemberRepository.findDistinctDisplayColorsByUserId(userId));

        // 기본은 팀 대표색
        if (!used.contains(teamColor)) return teamColor;

        // 대표색이 이미 사용 중이면, 중복 제외 후보들로 랜덤 선택
        List<TeamColor> candidates = Arrays.stream(TeamColor.values())
                .filter(c -> !used.contains(c))
                .toList();

        if (candidates.isEmpty()) {
            // 유저가 모든 색을 이미 사용 중인 극단 케이스: 그냥 팀색 유지
            return teamColor;
        }
        return candidates.get(RANDOM.nextInt(candidates.size()));
    }

    private List<TeamMemberResponse> buildMemberResponses(List<TeamMember> members) {
        List<Long> userIds = members.stream().map(TeamMember::getUserId).distinct().toList();
        Map<Long, User> userMap = userRepository.findAllById(userIds).stream()
                .collect(Collectors.toMap(User::getId, u -> u));

        return members.stream().map(m -> {
            User u = userMap.get(m.getUserId());
            TeamColor dc = m.getDisplayColor();
            return TeamMemberResponse.builder()
                    .userId(m.getUserId())
                    .nickname(u != null ? u.getNickname() : null)
                    .profileImageUrl(u != null ? u.getProfileImageUrl() : null)
                    .universityName(resolveUniversityName(u))
                    .role(m.getRole())
                    .joinedAt(m.getJoinedAt())
                    .displayColor(dc)
                    .displayColorHex(dc != null ? dc.getHex() : null)
                    .build();
        }).toList();
    }

    private String resolveUniversityName(User u) {
        if (u == null) return null;

        try {
            var uni = u.getUniversity();
            if (uni == null) return null;
            return uni.getName();
        } catch (Exception ignored) {
            try {
                return (String) u.getClass().getMethod("getUniversityName").invoke(u);
            } catch (Exception e) {
                return null;
            }
        }
    }

    private String generateUnique6DigitCode() {
        for (int i = 0; i < 20; i++) {
            String code = String.format("%06d", RANDOM.nextInt(1_000_000));
            if (!teamRepository.existsByInviteCode(code)) {
                return code;
            }
        }
        throw new TeamException("INVITE_CODE_GENERATION_FAILED", "초대코드 생성에 실패했습니다", 500);
    }

    private void requireLeader(Long teamId, Long userId, Team team) {
        if (team.getOwnerUserId() != null && !Objects.equals(team.getOwnerUserId(), userId)) {
            throw new TeamException(TeamErrorCodes.FORBIDDEN, "팀장만 가능한 작업입니다", 403);
        }

        TeamMember me = teamMemberRepository.findByTeamIdAndUserId(teamId, userId)
                .orElseThrow(() -> new TeamException(TeamErrorCodes.NOT_A_MEMBER, "팀 멤버가 아닙니다", 403));

        if (me.getRole() != TeamRole.LEADER) {
            throw new TeamException(TeamErrorCodes.FORBIDDEN, "팀장만 가능한 작업입니다", 403);
        }
    }

    private TeamResponse toTeamResponse(@NotNull Team team) {
        return TeamResponse.builder()
                .id(team.getId())
                .name(team.getName())
                .description(team.getDescription())
                .color(team.getColor())
                .colorHex(team.getColor() != null ? team.getColor().getHex() : null)
                .imageUrl(team.getImageUrl())
                .ownerUserId(team.getOwnerUserId())
                .startAt(team.getStartAt())
                .endAt(team.getEndAt())
                .isCompleted(isCompleted(team.getEndAt()))
                .createdAt(team.getCreatedAt())
                .updatedAt(team.getUpdatedAt())
                .build();
    }

    private void validateTeamPeriod(LocalDateTime startAt, LocalDateTime endAt) {
        if (startAt == null || endAt == null) {
            throw new TeamException(TeamErrorCodes.FORBIDDEN, "startAt/endAt은 필수입니다", 400);
        }
        if (endAt.isBefore(startAt)) {
            throw new TeamException(TeamErrorCodes.FORBIDDEN, "endAt은 startAt보다 빠를 수 없습니다", 400);
        }
    }

    private boolean isCompleted(LocalDateTime endAt) {
        return endAt != null && endAt.isBefore(LocalDateTime.now());
    }

    @Transactional(readOnly = true)
    public TeamInviteCodeResponse getInviteCode(Long userId, Long teamId) {
        Team team = teamRepository.findById(teamId)
                .orElseThrow(() -> new TeamException(
                        TeamErrorCodes.TEAM_NOT_FOUND, "팀을 찾을 수 없습니다", 404
                ));

        // 팀 멤버만 조회 가능
        if (!teamMemberRepository.existsByTeamIdAndUserId(teamId, userId)) {
            throw new TeamException(TeamErrorCodes.NOT_A_MEMBER, "팀 멤버만 접근할 수 있습니다", 403);
        }

        if (team.getInviteCode() == null || team.getInviteCode().isBlank() || team.getInviteCodeExpiresAt() == null) {
            throw new TeamException(
                    TeamErrorCodes.INVITE_CODE_NOT_ISSUED, "초대코드가 아직 발급되지 않았습니다", 404
            );
        }

        if (team.getInviteCodeExpiresAt().isBefore(LocalDateTime.now())) {
            throw new TeamException(
                    TeamErrorCodes.INVITE_CODE_EXPIRED, "초대코드가 만료되었습니다", 400
            );
        }

        return TeamInviteCodeResponse.builder()
                .teamId(teamId)
                .inviteCode(team.getInviteCode())
                .expiresAt(team.getInviteCodeExpiresAt())
                .build();
    }
}
