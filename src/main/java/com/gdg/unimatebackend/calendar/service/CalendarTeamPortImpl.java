package com.gdg.unimatebackend.calendar.service;

import com.gdg.unimatebackend.team.dto.TeamMemberResponse;
import com.gdg.unimatebackend.team.dto.TeamSummaryResponse;
import com.gdg.unimatebackend.team.service.TeamService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.*;

@Component
@RequiredArgsConstructor
public class CalendarTeamPortImpl implements CalendarTeamPort {

    private final TeamService teamService;

    @Override
    public List<Long> getMyTeamIds(Long userId) {
        List<TeamSummaryResponse> myTeams = teamService.getMyTeams(userId);

        List<Long> teamIds = new ArrayList<>();
        for (TeamSummaryResponse t : myTeams) {
            teamIds.add(t.getId()); // ✅ teamId가 아니라 id
        }
        return teamIds;
    }

    @Override
    public Map<Long, String> getTeamNames(Long userId, List<Long> teamIds) {
        List<TeamSummaryResponse> myTeams = teamService.getMyTeams(userId);

        Set<Long> want = new HashSet<>(teamIds);
        Map<Long, String> map = new HashMap<>();

        for (TeamSummaryResponse t : myTeams) {
            if (want.contains(t.getId())) {     // ✅ id
                map.put(t.getId(), t.getName());
            }
        }
        return map;
    }

    @Override
    public Set<Long> getTeamMemberUserIds(Long userId, List<Long> teamIds) {
        Set<Long> userIds = new HashSet<>();

        for (Long teamId : teamIds) {
            List<TeamMemberResponse> members = teamService.getTeamMembers(userId, teamId);
            for (TeamMemberResponse m : members) {
                userIds.add(m.getUserId());
            }
        }
        return userIds;
    }
}