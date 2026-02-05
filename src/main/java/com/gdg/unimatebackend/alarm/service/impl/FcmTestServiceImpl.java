package com.gdg.unimatebackend.alarm.service.impl;

import com.gdg.unimatebackend.alarm.dto.FcmSendDto;
import com.gdg.unimatebackend.alarm.dto.FcmSendRequest;
import com.gdg.unimatebackend.alarm.dto.FcmTestResponse;
import com.gdg.unimatebackend.alarm.entity.FcmDeviceToken;
import com.gdg.unimatebackend.alarm.repository.FcmDeviceTokenRepository;
import com.gdg.unimatebackend.alarm.service.FcmService;
import com.gdg.unimatebackend.alarm.service.FcmTestService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;

import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class FcmTestServiceImpl implements FcmTestService {

    private static final String DEFAULT_TITLE = "Unimate Test";
    private static final String DEFAULT_BODY  = "서버 템플릿 발송 테스트";

    private final FcmDeviceTokenRepository tokenRepository;
    private final FcmService fcmService;

    @Override
    public Long extractUserId(Authentication authentication) {
        if (authentication == null) return -1L;

        // 0) 가장 신뢰: authentication.getName()
        try {
            Long parsed = parsePositiveLong(authentication.getName());
            if (parsed != null) return parsed;
        } catch (Exception ignored) {}

        Object p = authentication.getPrincipal();
        if (p == null) return -1L;

        // 1) principal이 문자열인 경우
        if (p instanceof String s) {
            Long parsed = parsePositiveLong(s);
            return parsed != null ? parsed : -1L;
        }

        // 2) principal이 Map(claims)인 경우
        if (p instanceof Map<?, ?> map) {
            Long parsed = parsePositiveLong(toStr(map.get("sub")));
            if (parsed != null) return parsed;

            parsed = parsePositiveLong(toStr(map.get("id")));
            if (parsed != null) return parsed;

            parsed = parsePositiveLong(toStr(map.get("userId")));
            if (parsed != null) return parsed;
        }

        // 3) 커스텀 principal 메서드들
        for (String methodName : new String[]{"getUserId", "getId", "getMemberId"}) {
            try {
                var m = p.getClass().getMethod(methodName);
                Long parsed = parsePositiveLong(toStr(m.invoke(p)));
                if (parsed != null) return parsed;
            } catch (Exception ignored) {}
        }

        return -1L;
    }

    @Override
    public FcmTestResponse sendTestToUser(Long userId, FcmSendRequest request) {
        if (userId == null || userId <= 0) {
            return FcmTestResponse.builder()
                    .success(false)
                    .message("Invalid userId from JWT principal")
                    .detail("userId 추출 실패: authentication.getName() / principal 구조 확인 필요")
                    .build();
        }

        // 1) 활성 토큰 조회
        FcmDeviceToken tokenEntity = tokenRepository
                .findTopByUserIdAndIsActiveTrueOrderByUpdatedAtDesc(userId)
                .orElse(null);

        if (tokenEntity == null || tokenEntity.getToken() == null || tokenEntity.getToken().isBlank()) {
            return FcmTestResponse.builder()
                    .success(false)
                    .message("No active FCM token for this user")
                    .detail("먼저 /api/v1/fcm/token/me 로 토큰 등록 필요")
                    .build();
        }

        String token = tokenEntity.getToken().trim();

        // 2) request에서 title/body 반영 (없으면 기본값)
        String title = (request == null || isBlank(request.getTitle())) ? DEFAULT_TITLE : request.getTitle().trim();
        String body  = (request == null || isBlank(request.getBody()))  ? DEFAULT_BODY  : request.getBody().trim();

        try {
            String result = fcmService.sendMessageTo(
                    FcmSendDto.builder()
                            .token(token)
                            .title(title)
                            .body(body)
                            .build()
            );

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

    private boolean isBlank(String s) {
        return s == null || s.trim().isEmpty();
    }

    private String toStr(Object o) {
        return o == null ? null : String.valueOf(o);
    }

    private Long parsePositiveLong(String s) {
        if (s == null) return null;
        String t = s.trim();
        if (!t.matches("\\d+")) return null;
        long v = Long.parseLong(t);
        return v > 0 ? v : null;
    }

    private String safeMsg(String msg) {
        return msg == null ? "" : msg;
    }
}
