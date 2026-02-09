package com.gdg.unimatebackend.notification.service;

import com.gdg.unimatebackend.notification.entity.Notification;
import com.gdg.unimatebackend.notification.event.ScheduleAlarmEvent;
import com.gdg.unimatebackend.schedule.team.entity.TeamSchedule;
import com.gdg.unimatebackend.schedule.team.repository.TeamScheduleRepository;
import com.gdg.unimatebackend.team.entity.Team;
import com.gdg.unimatebackend.team.repository.TeamRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class ScheduleAlarmNotificationService {

    private final TeamScheduleRepository teamScheduleRepository;
    private final TeamRepository teamRepository;
    private final NotificationService notificationService;
    private final ApplicationEventPublisher eventPublisher;

    private static final Long SYSTEM_SENDER_ID = 0L;
    private static final ZoneId KST = ZoneId.of("Asia/Seoul");

    @Scheduled(cron = "0 * * * * *", zone = "Asia/Seoul")
    @Transactional
    public void generateMinuteAlarms() {
        LocalDateTime now = LocalDateTime.now(KST);
        LocalDateTime rangeEnd = now.plusDays(1); // alarmMinutes max 1440

        List<TeamSchedule> schedules =
                teamScheduleRepository.findByStartAtBetweenAndAlarmMinutesIsNotNull(now.minusMinutes(1), rangeEnd);

        if (schedules.isEmpty()) return;

        Map<Long, Team> teamMap = new HashMap<>();
        for (TeamSchedule s : schedules) {
            teamMap.computeIfAbsent(s.getTeamId(),
                    id -> teamRepository.findById(id).orElse(null));
        }

        for (TeamSchedule s : schedules) {
            Integer alarmMinutes = s.getAlarmMinutes();
            if (alarmMinutes == null) continue;

            long diffMinutes = ChronoUnit.MINUTES.between(now, s.getStartAt());
            if (diffMinutes != alarmMinutes) continue;

            Team team = teamMap.get(s.getTeamId());
            if (team == null) continue;

            String teamName = team.getName() != null ? team.getName() : "팀";
            String teamColorHex = (team.getColor() != null && team.getColor().getHex() != null)
                    ? team.getColor().getHex()
                    : "";

            String messageTitle = s.getTitle() != null ? s.getTitle() : "일정";
            String messageBody = String.format("'%s' 일정 시작 %d분 전입니다.", messageTitle, alarmMinutes);

            Long receiverId = s.getCreatedBy();
            if (receiverId == null) continue;

            String eventKey = "SCHEDULE_ALARM:" + s.getId() + ":" + receiverId + ":" + alarmMinutes + ":" + s.getStartAt();

            Notification notification = Notification.builder()
                    .eventKey(eventKey)
                    .type("SCHEDULE_ALARM")
                    .alarmType("ALARM_MINUTES")
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

            eventPublisher.publishEvent(ScheduleAlarmEvent.builder()
                    .notificationId(saved.getId())
                    .receiverId(receiverId)
                    .teamId(s.getTeamId())
                    .teamName(teamName)
                    .teamColorHex(teamColorHex)
                    .teamScheduleId(s.getId())
                    .alarmMinutes(alarmMinutes)
                    .messageTitle(messageTitle)
                    .messageBody(messageBody)
                    .createdAt(saved.getCreatedAt())
                    .build());
        }
    }
}
