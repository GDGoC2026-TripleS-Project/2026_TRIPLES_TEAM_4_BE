package com.gdg.unimatebackend.notification.service;

import com.gdg.unimatebackend.notification.entity.Notification;
import com.gdg.unimatebackend.schedule.team.entity.TeamSchedule;
import com.gdg.unimatebackend.schedule.team.repository.TeamScheduleRepository;
import com.gdg.unimatebackend.team.entity.Team;
import com.gdg.unimatebackend.team.entity.TeamMember;
import com.gdg.unimatebackend.team.repository.TeamMemberRepository;
import com.gdg.unimatebackend.team.repository.TeamRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.*;

@Slf4j
@Service
@RequiredArgsConstructor
public class DdayNotificationService {

    private final TeamScheduleRepository teamScheduleRepository;
    private final TeamRepository teamRepository;
    private final TeamMemberRepository teamMemberRepository;
    private final NotificationService notificationService;

    private static final int[] DDAYS = {1, 3, 7};

    @Scheduled(cron = "0 0 9 * * *", zone = "Asia/Seoul")
    @Transactional
    public void generateDailyDdays() {
        LocalDate today = LocalDate.now();
        for (int dday : DDAYS) {
            LocalDate target = today.plusDays(dday);
            generateForDate(target, dday);
        }
    }

    @Transactional
    public void generateForDate(LocalDate targetDate, int dday) {
        LocalDateTime start = targetDate.atStartOfDay();
        LocalDateTime end = targetDate.plusDays(1).atStartOfDay();

        List<TeamSchedule> schedules = teamScheduleRepository.findByStartAtBetween(start, end);
        if (schedules.isEmpty()) return;

        Set<Long> teamIds = new HashSet<>();
        for (TeamSchedule s : schedules) teamIds.add(s.getTeamId());

        Map<Long, Team> teamMap = new HashMap<>();
        teamRepository.findAllById(teamIds).forEach(t -> teamMap.put(t.getId(), t));

        Map<Long, List<Long>> teamMemberIds = new HashMap<>();
        for (Long teamId : teamIds) {
            List<TeamMember> members = teamMemberRepository.findAllByTeamIdOrderByJoinedAtAsc(teamId);
            List<Long> userIds = members.stream().map(TeamMember::getUserId).distinct().toList();
            teamMemberIds.put(teamId, userIds);
        }

        for (TeamSchedule s : schedules) {
            Team team = teamMap.get(s.getTeamId());
            if (team == null) continue;

            List<Long> targetUserIds = teamMemberIds.getOrDefault(s.getTeamId(), List.of());
            if (targetUserIds.isEmpty()) continue;

            String teamName = team.getName() != null ? team.getName() : "팀";
            String teamColorHex = (team.getColor() != null && team.getColor().getHex() != null)
                    ? team.getColor().getHex()
                    : "";

            String messageTitle = String.format("[D-%d] %s 마감이 %d일 남았습니다!", dday, s.getTitle(), dday);
            String messageBody = "진행 상황을 팀원들과 공유해보세요.";

            String eventKey = "DDAY:" + s.getId() + ":" + dday;

            Notification notification = Notification.builder()
                    .eventKey(eventKey)
                    .type("DDAY")
                    .alarmType("D-" + dday)
                    .teamId(s.getTeamId())
                    .teamName(teamName)
                    .teamColorHex(teamColorHex)
                    .messageTitle(messageTitle)
                    .messageBody(messageBody)
                    .build();

            notificationService.createNotificationWithReceipts(notification, targetUserIds);
        }
    }
}
