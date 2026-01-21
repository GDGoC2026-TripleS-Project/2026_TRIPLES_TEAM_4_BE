package com.gdg.unimatebackend.app.alarm.service.impl;

import com.gdg.unimatebackend.app.alarm.dto.FcmSendDto;
import com.gdg.unimatebackend.app.alarm.dto.FcmTestResponse;
import com.gdg.unimatebackend.app.alarm.entity.FcmDeviceToken;
import com.gdg.unimatebackend.app.alarm.repository.FcmDeviceTokenRepository;
import com.gdg.unimatebackend.app.alarm.service.FcmService;
import com.gdg.unimatebackend.app.alarm.service.FcmTestService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class FcmTestServiceImpl implements FcmTestService {

    private final FcmDeviceTokenRepository tokenRepository;
    private final FcmService fcmService;

    @Override
    public Long extractUserId(Authentication authentication) {
        // ✅ 너 프로젝트에서 principal이 어떤 타입인지에 따라 여기만 바꾸면 됨.
        // 예: principal이 CustomUserDetails 이면 getId()
        Object p = authentication.getPrincipal();

        // 1) principal이 String("1") 처럼 들어오는 경우
        if (p instanceof String s) {
            return Long.parseLong(s);
        }

        // 2) principal이 org.springframework.security.core.userdetails.User 같은 경우는 id 없음
        //    → 보통 커스텀 principal을 쓰는 게 맞음
        //    아래는 “리플렉션으로 getId() 있으면 가져오기” (임시로 매우 강력)
        try {
            var m = p.getClass().getMethod("getId");
            Object id = m.invoke(p);
            if (id instanceof Long l) return l;
            if (id instanceof Integer i) return i.longValue();
            if (id instanceof String s2) return Long.parseLong(s2);
        } catch (Exception ignored) {}

        // 3) 못 뽑으면 명확하게 실패 응답을 내리게 컨트롤러에서 처리할 수도 있음.
        //    여기서는 그냥 예외를 던지지 않고 “-1”로 보내고 아래에서 응답 처리.
        return -1L;
    }

    @Override
    public FcmTestResponse sendTestToUser(Long userId) {
        if (userId == null || userId <= 0) {
            return FcmTestResponse.builder()
                    .success(false)
                    .message("Invalid userId from JWT principal")
                    .detail("principal에서 userId 추출 실패. extractUserId 로직을 프로젝트 principal에 맞게 수정 필요")
                    .build();
        }

        // ✅ 1) DB에서 활성 토큰 조회 (없으면 500 대신 친절한 응답)
        FcmDeviceToken tokenEntity = tokenRepository
                .findTopByUserIdAndIsActiveTrueOrderByUpdatedAtDesc(userId)
                .orElse(null);

        if (tokenEntity == null || tokenEntity.getToken() == null || tokenEntity.getToken().isBlank()) {
            return FcmTestResponse.builder()
                    .success(false)
                    .message("No active FCM token for this user")
                    .detail("먼저 /api/v1/fcm/token/me 로 토큰 등록이 필요함")
                    .build();
        }

        String token = tokenEntity.getToken().trim();

        // ✅ 2) FCM 전송(여기서도 IOException이 나면 500 대신 응답으로 반환)
        try {
            String result = fcmService.sendMessageTo(
                    FcmSendDto.builder()
                            .token(token)
                            .title("Unimate Test")
                            .body("Hello from /api/v1/fcm/test/me")
                            .build()
            );

            return FcmTestResponse.builder()
                    .success(true)
                    .message("FCM send attempted")
                    .detail(result)
                    .build();

        } catch (Exception e) {
            log.error("[FCM] test/me failed. userId={}, err={}", userId, e.toString(), e);
            return FcmTestResponse.builder()
                    .success(false)
                    .message("FCM send failed (server-side)")
                    .detail(e.getClass().getSimpleName() + ": " + e.getMessage())
                    .build();
        }
    }
}
