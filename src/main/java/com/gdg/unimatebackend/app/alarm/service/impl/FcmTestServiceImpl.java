// ===============================
// FcmTestServiceImpl.java (FULL)
// ===============================
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

import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class FcmTestServiceImpl implements FcmTestService {

    private final FcmDeviceTokenRepository tokenRepository;
    private final FcmService fcmService;

    @Override
    public Long extractUserId(Authentication authentication) {
        if (authentication == null) return -1L;

        // 0) 가장 신뢰: authentication.getName() (우리 프로젝트는 userId를 name에 넣는 형태가 깔끔)
        try {
            String name = authentication.getName();
            Long parsed = parsePositiveLong(name);
            if (parsed != null) return parsed;
        } catch (Exception ignored) {}

        Object p = authentication.getPrincipal();
        if (p == null) return -1L;

        // 1) principal이 "1" 같은 문자열인 경우
        if (p instanceof String s) {
            Long parsed = parsePositiveLong(s);
            return parsed != null ? parsed : -1L;
        }

        // 2) principal이 Map / claims 처럼 sub를 들고 있는 경우
        if (p instanceof Map<?, ?> map) {
            Object sub = map.get("sub");
            Long parsed = parsePositiveLong(sub == null ? null : sub.toString());
            if (parsed != null) return parsed;

            // 혹시 키가 다른 경우까지 방어적으로
            Object id = map.get("id");
            parsed = parsePositiveLong(id == null ? null : id.toString());
            if (parsed != null) return parsed;

            Object userId = map.get("userId");
            parsed = parsePositiveLong(userId == null ? null : userId.toString());
            if (parsed != null) return parsed;
        }

        // 3) 커스텀 principal에 getUserId()/getId()/getMemberId()가 있는 경우 리플렉션으로 대응
        for (String methodName : new String[]{"getUserId", "getId", "getMemberId"}) {
            try {
                var m = p.getClass().getMethod(methodName);
                Object id = m.invoke(p);
                Long parsed = parsePositiveLong(id == null ? null : id.toString());
                if (parsed != null) return parsed;
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
                    .detail("principal에서 userId 추출 실패. (authentication.getName / principal 구조 확인 필요)")
                    .build();
        }

        // 1) DB에서 활성 토큰 조회
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

        // 2) 전송
        try {
            String result = fcmService.sendMessageTo(
                    FcmSendDto.builder()
                            .token(token)
                            .title("Unimate Test")
                            .body("Hello from /api/v1/fcm/test/me")
                            .build()
            );

            // fcmService가 "ERROR: ..." 문자열을 리턴할 수 있으니, 성공 여부를 문자열로도 한 번 더 반영
            boolean ok = result != null && result.startsWith("OK:");

            return FcmTestResponse.builder()
                    .success(ok)
                    .message(ok ? "FCM send ok" : "FCM send failed (FCM response)")
                    .detail(result)
                    .build();

        } catch (Exception e) {
            log.error("[FCM] test/me failed. userId={}, err={}", userId, e.toString(), e);
            return FcmTestResponse.builder()
                    .success(false)
                    .message("FCM send failed (server-side)")
                    .detail(e.getClass().getSimpleName() + ": " + safeMsg(e.getMessage()))
                    .build();
        }
    }

    private Long parsePositiveLong(String s) {
        if (s == null) return null;
        String t = s.trim();
        if (!t.matches("\\d+")) return null;
        try {
            long v = Long.parseLong(t);
            return v > 0 ? v : null;
        } catch (Exception e) {
            return null;
        }
    }

    private String safeMsg(String msg) {
        return msg == null ? "" : msg;
    }
}
