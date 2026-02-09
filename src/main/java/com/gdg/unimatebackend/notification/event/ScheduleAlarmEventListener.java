package com.gdg.unimatebackend.notification.event;

import com.gdg.unimatebackend.alarm.dto.FcmSendDto;
import com.gdg.unimatebackend.alarm.repository.FcmDeviceTokenRepository;
import com.gdg.unimatebackend.alarm.service.FcmService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.HashMap;
import java.util.Map;

@Slf4j
@Component
@RequiredArgsConstructor
public class ScheduleAlarmEventListener {

    private final FcmDeviceTokenRepository fcmDeviceTokenRepository;
    private final FcmService fcmService;

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT, fallbackExecution = true)
    public void onScheduleAlarm(ScheduleAlarmEvent event) {
        Long receiverId = event.getReceiverId();
        if (receiverId == null) return;

        var tokenOpt = fcmDeviceTokenRepository
                .findTopByUserIdAndIsActiveTrueOrderByUpdatedAtDesc(receiverId);

        if (tokenOpt.isEmpty()) {
            log.info("[ALARM][FCM] no token. receiverId={}", receiverId);
            return;
        }

        LocalDateTime createdAt = event.getCreatedAt() != null
                ? event.getCreatedAt()
                : LocalDateTime.now();
        String createdAtText = createdAt.atOffset(ZoneOffset.ofHours(9)).toString();

        Map<String, String> data = new HashMap<>();
        data.put("notificationId", String.valueOf(event.getNotificationId()));
        data.put("type", "SCHEDULE_ALARM");
        data.put("receiverId", String.valueOf(receiverId));
        data.put("teamId", String.valueOf(event.getTeamId()));
        data.put("teamName", event.getTeamName() != null ? event.getTeamName() : "");
        data.put("teamColorHex", event.getTeamColorHex() != null ? event.getTeamColorHex() : "");
        data.put("teamScheduleId", String.valueOf(event.getTeamScheduleId()));
        data.put("alarmMinutes", String.valueOf(event.getAlarmMinutes()));
        data.put("messageTitle", event.getMessageTitle());
        data.put("messageBody", event.getMessageBody());
        data.put("createdAt", createdAtText);

        try {
            String pushTitle = event.getPushTitle() != null ? event.getPushTitle() : event.getMessageTitle();
            String pushBody = event.getPushBody() != null ? event.getPushBody() : event.getMessageBody();
            if (pushBody == null || pushBody.isBlank()) {
                pushBody = event.getMessageTitle();
            }
            fcmService.sendMessageTo(FcmSendDto.builder()
                    .token(tokenOpt.get().getToken())
                    .title(pushTitle)
                    .body(pushBody)
                    .data(data)
                    .build());
        } catch (Exception e) {
            log.warn("[ALARM][FCM] fail receiverId={}, reason={}", receiverId, e.getMessage());
        }
    }
}
