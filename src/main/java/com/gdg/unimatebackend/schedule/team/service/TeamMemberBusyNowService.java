package com.gdg.unimatebackend.schedule.team.service;

import com.gdg.unimatebackend.schedule.repository.MyScheduleRepository;
import com.gdg.unimatebackend.schedule.team.dto.TeamMemberBusyNowResponse;
import com.gdg.unimatebackend.schedule.team.exception.TeamScheduleErrorCodes;
import com.gdg.unimatebackend.schedule.team.exception.TeamScheduleException;
import com.gdg.unimatebackend.team.entity.TeamMember;
import com.gdg.unimatebackend.team.repository.TeamMemberRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class TeamMemberBusyNowService {

    private final TeamMemberRepository teamMemberRepository;
    private final MyScheduleRepository myScheduleRepository;

    @Transactional(readOnly = true)
    public TeamMemberBusyNowResponse getBusyNow(Long userId, Long teamId) {
        requireMember(teamId, userId);

        List<TeamMember> members = teamMemberRepository.findAllByTeamIdOrderByJoinedAtAsc(teamId);
        List<Long> userIds = members.stream().map(TeamMember::getUserId).distinct().toList();

        LocalDateTime now = LocalDateTime.now();
        List<Long> busyUserIds = myScheduleRepository.findBusyUserIdsNow(teamId, userIds, now);

        Set<Long> busySet = new HashSet<>(busyUserIds);

        List<TeamMemberBusyNowResponse.MemberBusy> result = userIds.stream()
                .map(uid -> new TeamMemberBusyNowResponse.MemberBusy(uid, busySet.contains(uid)))
                .toList();

        return new TeamMemberBusyNowResponse(result);
    }

    private void requireMember(Long teamId, Long userId) {
        if (!teamMemberRepository.existsByTeamIdAndUserId(teamId, userId)) {
            throw new TeamScheduleException(TeamScheduleErrorCodes.NOT_A_MEMBER, "팀 멤버만 접근할 수 있습니다", 403);
        }
    }
}
