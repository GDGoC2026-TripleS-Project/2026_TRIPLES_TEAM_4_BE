package com.gdg.unimatebackend.app.team.service;

import com.gdg.unimatebackend.app.team.dto.*;
import com.gdg.unimatebackend.app.team.entity.*;
import com.gdg.unimatebackend.app.team.exception.TeamException;
import com.gdg.unimatebackend.app.team.exception.TeamErrorCodes;
import com.gdg.unimatebackend.app.team.repository.TeamMemberRepository;
import com.gdg.unimatebackend.app.team.repository.TeamRepository;
import com.gdg.unimatebackend.app.user.entity.User;
import com.gdg.unimatebackend.app.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
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

    private static final SecureRandom RANDOM = new SecureRandom();

    // ===== 팀 생성 =====
    @Transactional
    public TeamResponse createTeam(Long userId, TeamCreateRequest request) {
        Team team = Team.builder()
                .name(request.getName())
                .description(request.getDescription())
                .color(request.getColor())
                .ownerUserId(userId)
                .build();

        team = teamRepository.save(team);

        // 생성자는 자동 LEADER 가입
        teamMemberRepository.save(TeamMember.builder()
                .teamId(team.getId())
                .userId(userId)
                .role(TeamRole.LEADER)
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

        List<TeamSummaryResponse> result = new ArrayList<>();
        for (Long teamId : teamIdsOrdered) {
            Team t = teamMap.get(teamId);
            if (t == null) continue;

            long memberCount = teamMemberRepository.countByTeamId(teamId);
            TeamRole myRole = myRoleMap.get(teamId);

            result.add(TeamSummaryResponse.builder()
                    .id(t.getId())
                    .name(t.getName())
                    .description(t.getDescription())
                    .color(t.getColor())
                    .myRole(myRole)
                    .memberCount(memberCount)
                    .createdAt(t.getCreatedAt())
                    .updatedAt(t.getUpdatedAt())
                    .build());
        }
        return result;
    }

    // ===== 팀 상세(q3 반영) =====
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

        return TeamDetailResponse.builder()
                .team(toTeamResponse(team))
                .members(memberResponses)
                .myRole(me.getRole())
                .memberCount(members.size())
                .build();
    }

    // ===== 팀 정보 수정 =====
    @Transactional
    public TeamResponse updateTeam(Long userId, Long teamId, TeamUpdateRequest request) {
        Team team = teamRepository.findById(teamId)
                .orElseThrow(() -> new TeamException(
                        TeamErrorCodes.TEAM_NOT_FOUND, "팀을 찾을 수 없습니다", 404
                ));

        requireLeader(teamId, userId, team);

        team.update(request.getName(), request.getDescription(), request.getColor());
        return toTeamResponse(team);
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
    }

    // ===== 팀원 목록 조회 =====
    @Transactional(readOnly = true)
    public List<TeamMemberResponse> getTeamMembers(Long userId, Long teamId) {
        // 멤버만 조회 가능
        if (!teamMemberRepository.existsByTeamIdAndUserId(teamId, userId)) {
            throw new TeamException(TeamErrorCodes.NOT_A_MEMBER, "팀 멤버만 접근할 수 있습니다", 403);
        }

        List<TeamMember> members = teamMemberRepository.findAllByTeamIdOrderByJoinedAtAsc(teamId);
        return buildMemberResponses(members);
    }

    // ===== 초대코드 발급/재발급 (teams 컬럼 사용, 팀장만, 10분) =====
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

    // ===== 초대코드로 팀 참여 (teams 컬럼 사용) =====
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

        // ✅ 람다 트릭 제거 (createdHolder 같은 거 필요 없음)
        Optional<TeamMember> existing = teamMemberRepository.findByTeamIdAndUserId(teamId, userId);

        TeamMember me;
        if (existing.isPresent()) {
            me = existing.get();
        } else {
            me = teamMemberRepository.save(
                    TeamMember.builder()
                            .teamId(teamId)
                            .userId(userId)
                            .role(TeamRole.MEMBER)
                            .build()
            );
        }

        List<TeamMember> members = teamMemberRepository.findAllByTeamIdOrderByJoinedAtAsc(teamId);
        List<TeamMemberResponse> memberResponses = buildMemberResponses(members);

        return TeamJoinResponse.builder()
                .team(toTeamResponse(team))
                .myRole(me.getRole())
                .memberCount(members.size())
                .members(memberResponses)
                .build();
    }

    // ===== helpers =====

    private List<TeamMemberResponse> buildMemberResponses(List<TeamMember> members) {
        List<Long> userIds = members.stream().map(TeamMember::getUserId).distinct().toList();
        Map<Long, User> userMap = userRepository.findAllById(userIds).stream()
                .collect(Collectors.toMap(User::getId, u -> u));

        return members.stream().map(m -> {
            User u = userMap.get(m.getUserId());
            return TeamMemberResponse.builder()
                    .userId(m.getUserId())
                    .nickname(u != null ? u.getNickname() : null)
                    .profileImageUrl(u != null ? u.getProfileImageUrl() : null)
                    .universityName(resolveUniversityName(u))
                    .role(m.getRole())
                    .joinedAt(m.getJoinedAt())
                    .build();
        }).toList();
    }

    /**
     * ✅ 여기만 네 User 구조에 맞추면 'u.getUniversityName() 오류'가 사라짐
     * - User에 getUniversity() 연관관계가 있으면: u.getUniversity().getName()
     * - 문자열 필드면: u.getUniversityName()
     */
    private String resolveUniversityName(User u) {
        if (u == null) return null;

        // 1) 연관관계형 (가장 흔함)
        try {
            var uni = u.getUniversity(); // User에 getUniversity()가 없으면 catch로 이동
            if (uni == null) return null;
            return uni.getName();
        } catch (Exception ignored) {
            // 2) 문자열 필드형으로 fallback
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
        // 팀 생성자 기준 1차 체크
        if (team.getOwnerUserId() != null && !Objects.equals(team.getOwnerUserId(), userId)) {
            throw new TeamException(TeamErrorCodes.FORBIDDEN, "팀장만 가능한 작업입니다", 403);
        }

        // 멤버십/역할 체크
        TeamMember me = teamMemberRepository.findByTeamIdAndUserId(teamId, userId)
                .orElseThrow(() -> new TeamException(TeamErrorCodes.NOT_A_MEMBER, "팀 멤버가 아닙니다", 403));

        if (me.getRole() != TeamRole.LEADER) {
            throw new TeamException(TeamErrorCodes.FORBIDDEN, "팀장만 가능한 작업입니다", 403);
        }
    }

    private TeamResponse toTeamResponse(Team team) {
        return TeamResponse.builder()
                .id(team.getId())
                .name(team.getName())
                .description(team.getDescription())
                .color(team.getColor())
                .ownerUserId(team.getOwnerUserId())
                .createdAt(team.getCreatedAt())
                .updatedAt(team.getUpdatedAt())
                .build();
    }
}
