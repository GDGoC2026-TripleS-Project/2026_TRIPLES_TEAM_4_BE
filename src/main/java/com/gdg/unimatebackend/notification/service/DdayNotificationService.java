package com.gdg.unimatebackend.notification.service;

import com.gdg.unimatebackend.notification.entity.DdayMessageTemplate;
import com.gdg.unimatebackend.notification.entity.Notification;
import com.gdg.unimatebackend.notification.repository.DdayMessageTemplateRepository;
import com.gdg.unimatebackend.notification.event.DdayNotificationEvent;
import com.gdg.unimatebackend.schedule.team.entity.TeamSchedule;
import com.gdg.unimatebackend.schedule.team.repository.TeamScheduleRepository;
import com.gdg.unimatebackend.team.entity.Team;
import com.gdg.unimatebackend.team.entity.TeamMember;
import com.gdg.unimatebackend.team.repository.TeamMemberRepository;
import com.gdg.unimatebackend.team.repository.TeamRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.*;

@Slf4j
@Service
@RequiredArgsConstructor
public class DdayNotificationService {

    private final TeamScheduleRepository teamScheduleRepository;
    private final TeamRepository teamRepository;
    private final TeamMemberRepository teamMemberRepository;
    private final NotificationService notificationService;
    private final DdayMessageTemplateRepository ddayMessageTemplateRepository;
    private final ApplicationEventPublisher eventPublisher;

    private static final Long SYSTEM_SENDER_ID = 0L;

    @Scheduled(cron = "0 0 9 * * *", zone = "Asia/Seoul")
    @Transactional
    public void generateDailyDdays() {
        LocalDate today = LocalDate.now();
        Map<Integer, DdayMessageTemplate> templateMap = loadTemplates();
        LocalDateTime rangeStart = today.atStartOfDay();
        LocalDateTime rangeEnd = today.plusDays(8).atStartOfDay(); // D-1/3/7 범위

        List<TeamSchedule> schedules = teamScheduleRepository.findByEndAtBetween(rangeStart, rangeEnd);
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
            long diff = ChronoUnit.DAYS.between(today, s.getEndAt().toLocalDate());
            if (diff != 1 && diff != 3 && diff != 7) continue;

            int dday = (int) diff;
            DdayMessageTemplate template = templateMap.get(dday);

            Team team = teamMap.get(s.getTeamId());
            if (team == null) continue;

            List<Long> targetUserIds = teamMemberIds.getOrDefault(s.getTeamId(), List.of());
            if (targetUserIds.isEmpty()) continue;

            String teamName = team.getName() != null ? team.getName() : "팀";
            String teamColorHex = (team.getColor() != null && team.getColor().getHex() != null)
                    ? team.getColor().getHex()
                    : "";

            String messageTitle;
            String messageBody;
            if (template != null) {
                messageTitle = renderTemplate(template.getTitleTemplate(), s, team, dday);
                messageBody = renderTemplate(template.getBodyTemplate(), s, team, dday);
            } else {
                messageTitle = String.format("[D-%d] %s 마감이 %d일 남았습니다!", dday, s.getTitle(), dday);
                messageBody = "진행 상황을 팀원들과 공유해보세요.";
            }

            for (Long receiverId : targetUserIds) {
                String eventKey = "DDAY:" + s.getId() + ":" + receiverId + ":" + dday;

                Notification notification = Notification.builder()
                        .eventKey(eventKey)
                        .type("DDAY")
                        .alarmType("D-" + dday)
                        .teamId(s.getTeamId())
                        .teamName(teamName)
                        .teamColorHex(teamColorHex)
                        .messageTitle(messageTitle)
                        .messageBody(messageBody)
                        .senderId(SYSTEM_SENDER_ID)
                        .receiverId(receiverId)
                        .teamScheduleId(s.getId())
                        .build();

                Notification saved = notificationService.createNotificationWithReceipt(notification, receiverId);

                eventPublisher.publishEvent(DdayNotificationEvent.builder()
                        .notificationId(saved.getId())
                        .receiverId(receiverId)
                        .teamId(s.getTeamId())
                        .teamName(teamName)
                        .teamColorHex(teamColorHex)
                        .teamScheduleId(s.getId())
                        .dday(dday)
                        .messageTitle(messageTitle)
                        .messageBody(messageBody)
                        .createdAt(saved.getCreatedAt())
                        .build());
            }
        }
    }

    private Map<Integer, DdayMessageTemplate> loadTemplates() {
        List<DdayMessageTemplate> list = ddayMessageTemplateRepository.findAll();
        Map<Integer, DdayMessageTemplate> map = new HashMap<>();
        for (DdayMessageTemplate t : list) {
            map.put(t.getDday(), t);
        }
        if (map.isEmpty()) {
            log.warn("[DDAY] no templates found in DB. fallback to default messages.");
        }
        return map;
    }

    private String renderTemplate(String template, TeamSchedule schedule, Team team, int dday) {
        if (template == null) return "";
        String title = (schedule.getTitle() != null && !schedule.getTitle().isBlank())
                ? schedule.getTitle()
                : "과제";
        String teamName = (team.getName() != null && !team.getName().isBlank())
                ? team.getName()
                : "팀";
        return template
                .replace("{title}", title)
                .replace("{team}", teamName)
                .replace("{dday}", String.valueOf(dday));
    }
}
