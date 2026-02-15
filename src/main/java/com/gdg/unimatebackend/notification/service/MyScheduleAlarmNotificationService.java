package com.gdg.unimatebackend.notification.service;

import com.gdg.unimatebackend.notification.entity.Notification;
import com.gdg.unimatebackend.notification.event.MyScheduleAlarmEvent;
import com.gdg.unimatebackend.schedule.entity.MySchedule;
import com.gdg.unimatebackend.schedule.repository.MyScheduleRepository;
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
public class MyScheduleAlarmNotificationService {

    private final MyScheduleRepository myScheduleRepository;
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

        List<MySchedule> schedules =
                myScheduleRepository.findByEndAtBetweenAndAlarmMinutesIsNotNull(now.minusMinutes(1), rangeEnd);

        if (schedules.isEmpty()) return;

        Map<Long, Team> teamMap = new HashMap<>();
        for (MySchedule s : schedules) {
            teamMap.computeIfAbsent(s.getTeamId(),
                    id -> teamRepository.findById(id).orElse(null));
        }

        for (MySchedule s : schedules) {
            Integer alarmMinutes = s.getAlarmMinutes();
            if (alarmMinutes == null) continue;

            if (!shouldTriggerNow(now, s.getEndAt(), alarmMinutes)) continue;

            Team team = teamMap.get(s.getTeamId());
            if (team == null) continue;

            String teamName = team.getName() != null ? team.getName() : "팀";
            String teamColorHex = (team.getColor() != null && team.getColor().getHex() != null)
                    ? team.getColor().getHex()
                    : "";

            String rawTitle = s.getTitle() != null ? s.getTitle() : "일정";
            String messageTitle = String.format("%s 마감 시간이 %d분 남았습니다!", rawTitle, alarmMinutes);
            String messageBody = "";
            String pushTitle = rawTitle;
            String pushBody = String.format("'%s' 일정 마감 %d분 전입니다.", rawTitle, alarmMinutes);

            Long receiverId = s.getUserId();
            if (receiverId == null) continue;

            String eventKey = "SCHEDULE_ALARM:MY:" + s.getId() + ":" + receiverId + ":" + alarmMinutes + ":" + s.getEndAt();

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
                    .build();

            Notification saved = notificationService.createNotificationWithReceipt(notification, receiverId);

            eventPublisher.publishEvent(MyScheduleAlarmEvent.builder()
                    .notificationId(saved.getId())
                    .receiverId(receiverId)
                    .teamId(s.getTeamId())
                    .teamName(teamName)
                    .teamColorHex(teamColorHex)
                    .myScheduleId(s.getId())
                    .alarmMinutes(alarmMinutes)
                    .messageTitle(messageTitle)
                    .messageBody(messageBody)
                    .pushTitle(pushTitle)
                    .pushBody(pushBody)
                    .createdAt(saved.getCreatedAt())
                    .build());
        }
    }

    private boolean shouldTriggerNow(LocalDateTime now, LocalDateTime endAt, int alarmMinutes) {
        long diffSeconds = ChronoUnit.SECONDS.between(now, endAt);
        long upper = alarmMinutes * 60L;
        long lower = upper - 60L;
        return diffSeconds <= upper && diffSeconds > lower;
    }
}
