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
        // 0) 제일 우선: name()에 userId가 들어있는 경우 (권장)
        try {
            String name = authentication.getName();
            if (name != null && name.matches("\\d+")) {
                return Long.parseLong(name);
            }
        } catch (Exception ignored) {}

        Object p = authentication.getPrincipal();
        if (p == null) return -1L;

        // 1) principal이 "1" 같은 문자열인 경우
        if (p instanceof String s) {
            if (s.matches("\\d+")) return Long.parseLong(s);
            return -1L;
        }

        // 2) principal이 Map / claims 처럼 sub를 들고 있는 경우
        if (p instanceof java.util.Map<?, ?> map) {
            Object sub = map.get("sub");
            if (sub != null && sub.toString().matches("\\d+")) {
                return Long.parseLong(sub.toString());
            }
        }

        // 3) getUserId(), getId() 메서드가 있는 커스텀 principal 대응
        for (String methodName : new String[]{"getUserId", "getId"}) {
            try {
                var m = p.getClass().getMethod(methodName);
                Object id = m.invoke(p);
                if (id == null) continue;
                String s = id.toString();
                if (s.matches("\\d+")) return Long.parseLong(s);
            } catch (Exception ignored) {}
        }

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
