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

        String deviceId = normalizeDeviceId(request.getDeviceId());
        String platform = normalizePlatform(request.getPlatform());

        // ✅ A안: 유저당 활성 1개 (마지막 등록 디바이스만 수신)
        tokenRepository.deactivateAllByUserId(userId);

        // ✅ (user_id, device_id, platform) 기준 업서트
        tokenRepository.upsertByDevice(userId, token, deviceId, platform);

        log.info("[FCM] token registered. userId={}, platform={}, deviceId={}, tokenPrefix={}",
                userId, platform, deviceId, token.substring(0, Math.min(12, token.length())));
    }

    private String normalizeToken(String token) {
        if (token == null || token.trim().isEmpty()) {
            throw new IllegalArgumentException("FCM token is required");
        }
        return token.trim();
    }

    private String normalizeDeviceId(String v) {
        // 프론트가 deviceId를 안 보내면 "디바이스 단위 식별"이 불가능해짐.
        // 그래도 서버가 깨지지 않게 최소 폴백을 둔다.
        if (v == null || v.trim().isEmpty()) return "UNKNOWN";
        return v.trim();
    }

    private String normalizePlatform(String v) {
        if (v == null || v.trim().isEmpty()) return "UNKNOWN";
        return v.trim().toUpperCase();
    }

    private void validateNotJwtLike(String token) {
        long dots = token.chars().filter(c -> c == '.').count();
        if (token.startsWith("eyJ") && dots == 2) {
            throw new IllegalArgumentException("FCM token looks like JWT");
        }
    }
}
