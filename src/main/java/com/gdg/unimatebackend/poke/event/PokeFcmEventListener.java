package com.gdg.unimatebackend.poke.event;

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
public class PokeFcmEventListener {

    private final FcmDeviceTokenRepository fcmDeviceTokenRepository;
    private final FcmService fcmService;

    @TransactionalEventListener(
            phase = TransactionPhase.AFTER_COMMIT,
            fallbackExecution = true
    )
    public void handle(PokeFcmEvent event) {
        Long receiverId = event.getReceiverId();
        if (receiverId == null) return;

        var opt = fcmDeviceTokenRepository
                .findTopByUserIdAndIsActiveTrueOrderByUpdatedAtDesc(receiverId);

        if (opt.isEmpty()) {
            log.info("[POKE][FCM] no token. targetUserId={}", receiverId);
            return;
        }

        LocalDateTime createdAt = event.getCreatedAt() != null
                ? event.getCreatedAt()
                : LocalDateTime.now();
        String createdAtText = createdAt.atOffset(ZoneOffset.ofHours(9)).toString();

        Map<String, String> data = new HashMap<>();
        data.put("notificationId", String.valueOf(event.getNotificationId()));
        data.put("eventKey", event.getEventKey());
        data.put("type", "POKE");
        data.put("senderId", String.valueOf(event.getSenderId()));
        data.put("receiverId", String.valueOf(receiverId));
        data.put("pokeId", String.valueOf(event.getPokeId()));
        data.put("teamId", String.valueOf(event.getTeamId()));
        data.put("teamName", event.getTeamName() != null ? event.getTeamName() : "");
        data.put("teamColorHex", event.getTeamColorHex() != null ? event.getTeamColorHex() : "");
        data.put("alarmType", event.getAlarmType());
        data.put("messageTitle", event.getMessageTitle());
        data.put("messageBody", event.getMessageBody());
        data.put("createdAt", createdAtText);

        try {
            fcmService.sendMessageTo(FcmSendDto.builder()
                    .token(opt.get().getToken())
                    .title(event.getPushTitle())
                    .body(event.getPushBody())
                    .data(data)
                    .build());
        } catch (Exception e) {
            log.warn("[POKE][FCM] fail targetUserId={}, reason={}", receiverId, e.getMessage());
        }
    }
}
