package com.gdg.unimatebackend.app.poke.event;

import com.gdg.unimatebackend.app.alarm.dto.FcmSendDto;
import com.gdg.unimatebackend.app.alarm.repository.FcmDeviceTokenRepository;
import com.gdg.unimatebackend.app.alarm.service.FcmService;
import com.gdg.unimatebackend.app.poke.repository.PokeMessageRepository;
import com.gdg.unimatebackend.app.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Slf4j
@Component
@RequiredArgsConstructor
public class PokeAlarmEventListener {

    private final FcmDeviceTokenRepository fcmDeviceTokenRepository;
    private final FcmService fcmService;

    private final UserRepository userRepository;
    private final PokeMessageRepository pokeMessageRepository;

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handle(PokeSentEvent event) {
        String senderName = userRepository.findById(event.getSenderId())
                .map(u -> u.getNickname() == null || u.getNickname().isBlank() ? "팀원" : u.getNickname())
                .orElse("팀원");

        String content = pokeMessageRepository.findById(event.getPokeMessageId())
                .map(m -> m.getContent() == null ? "" : m.getContent())
                .orElse("");

        String title = "찌르기 도착 👀";
        String body = senderName + "님이 찌르기를 보냈어요\n" + content;

        for (Long targetUserId : event.getTargetUserIds()) {
            try {
                var opt = fcmDeviceTokenRepository
                        .findTopByUserIdAndIsActiveTrueOrderByUpdatedAtDesc(targetUserId);

                if (opt.isEmpty()) {
                    log.info("[POKE][FCM] no token. targetUserId={}", targetUserId);
                    continue;
                }

                fcmService.sendMessageTo(FcmSendDto.builder()
                        .token(opt.get().getToken())
                        .title(title)
                        .body(body)
                        .build());

            } catch (Exception e) {
                log.warn("[POKE][FCM] fail targetUserId={}, reason={}", targetUserId, e.getMessage());
            }
        }
    }
}
