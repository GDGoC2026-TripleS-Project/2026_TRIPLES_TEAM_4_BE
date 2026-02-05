package com.gdg.unimatebackend.schedule.team.service;

import com.gdg.unimatebackend.schedule.repository.MyScheduleRepository;
import com.gdg.unimatebackend.schedule.team.exception.TeamScheduleErrorCodes;
import com.gdg.unimatebackend.schedule.team.exception.TeamScheduleException;
import com.gdg.unimatebackend.team.dto.TeamIdleMemberResponse;
import com.gdg.unimatebackend.team.entity.TeamMember;
import com.gdg.unimatebackend.team.entity.TeamColor;
import com.gdg.unimatebackend.team.repository.TeamMemberRepository;
import com.gdg.unimatebackend.user.entity.User;
import com.gdg.unimatebackend.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class TeamMemberIdleService {

    private final TeamMemberRepository teamMemberRepository;
    private final MyScheduleRepository myScheduleRepository;
    private final UserRepository userRepository;

    @Transactional(readOnly = true)
    public List<TeamIdleMemberResponse> getIdleMembers(Long userId, Long teamId, LocalDate date) {
        requireMember(teamId, userId);
        if (date == null) {
            throw new TeamScheduleException(TeamScheduleErrorCodes.INVALID_RANGE, "date는 필수입니다", 400);
        }

        LocalDateTime rangeStart = date.atStartOfDay();
        LocalDateTime rangeEnd = date.plusDays(1).atStartOfDay();

        List<TeamMember> members = teamMemberRepository.findAllByTeamIdOrderByJoinedAtAsc(teamId);
        List<Long> memberUserIds = members.stream().map(TeamMember::getUserId).distinct().toList();

        List<Long> scheduledUserIds = myScheduleRepository.findUserIdsWithSchedulesInRange(
                teamId, rangeStart, rangeEnd
        );
        Set<Long> scheduledSet = new HashSet<>(scheduledUserIds);

        Map<Long, User> userMap = userRepository.findAllById(memberUserIds).stream()
                .collect(Collectors.toMap(User::getId, u -> u));

        List<TeamIdleMemberResponse> result = new ArrayList<>();
        for (TeamMember m : members) {
            if (scheduledSet.contains(m.getUserId())) continue;
            User u = userMap.get(m.getUserId());
            TeamColor dc = m.getDisplayColor();
            result.add(TeamIdleMemberResponse.builder()
                    .userId(m.getUserId())
                    .nickname(u != null ? u.getNickname() : null)
                    .profileImageUrl(u != null ? u.getProfileImageUrl() : null)
                    .displayColorHex(dc != null ? dc.getHex() : null)
                    .build());
        }
        return result;
    }

    private void requireMember(Long teamId, Long userId) {
        if (!teamMemberRepository.existsByTeamIdAndUserId(teamId, userId)) {
            throw new TeamScheduleException(TeamScheduleErrorCodes.NOT_A_MEMBER, "팀 멤버만 접근할 수 있습니다", 403);
        }
    }
}
