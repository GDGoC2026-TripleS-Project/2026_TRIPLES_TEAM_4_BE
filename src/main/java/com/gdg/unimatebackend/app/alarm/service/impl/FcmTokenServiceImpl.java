package com.gdg.unimatebackend.app.alarm.service.impl;

import com.gdg.unimatebackend.app.alarm.dto.FcmTokenRegisterRequest;
import com.gdg.unimatebackend.app.alarm.repository.FcmDeviceTokenRepository;
import com.gdg.unimatebackend.app.alarm.service.FcmTokenService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class FcmTokenServiceImpl implements FcmTokenService {

    private final FcmDeviceTokenRepository tokenRepository;

    @Override
    @Transactional
    public void register(Long userId, FcmTokenRegisterRequest request) {
        String token = normalizeToken(request.getToken());
        validateNotJwtLike(token);

        // ✅ deviceId/platform 방어 (빈값이면 upsert키가 흔들려서 토큰 갱신이 꼬일 수 있음)
        String deviceId = normalizeDeviceId(request.getDeviceId(), userId);
        String platform = normalizePlatform(request.getPlatform());

        // ✅ (A안) 유저당 활성 1개만 유지: 마지막 등록 디바이스만 수신
        // (중요) static 호출 금지. 주입받은 repository 인스턴스로 호출해야 함.
        tokenRepository.deactivateAllByUserId(userId);

        // ✅ (user_id, device_id, platform) 기준 UPSERT
        int affected = tokenRepository.upsertByDevice(userId, token, deviceId, platform);

        log.info("[FCM] token registered. userId={}, deviceId={}, platform={}, affected={}",
                userId, deviceId, platform, affected);
    }

    private String normalizeToken(String token) {
        if (token == null || token.trim().isEmpty()) {
            throw new IllegalArgumentException("FCM token is required");
        }
        return token.trim();
    }

    private String normalizeDeviceId(String deviceId, Long userId) {
        // 프론트가 deviceId를 안 보내면 "디바이스 단위 식별"이 불가능해짐.
        // 그래도 서버가 깨지지 않게 최소 폴백을 둔다.
        if (deviceId == null || deviceId.trim().isEmpty()) {
            return "unknown-" + userId; // user별로라도 고정되게
        }
        return deviceId.trim();
    }

    private String normalizePlatform(String platform) {
        if (platform == null || platform.trim().isEmpty()) {
            return "ANDROID"; // 네 정책대로 기본 ANDROID
        }
        return platform.trim().toUpperCase();
    }

    private void validateNotJwtLike(String token) {
        long dots = token.chars().filter(c -> c == '.').count();
        if (token.startsWith("eyJ") && dots == 2) {
            throw new IllegalArgumentException("FCM token looks like JWT");
        }
    }
}
