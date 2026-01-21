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
        String token = normalize(request.getToken());

        validateNotJwtLike(token);

        String deviceId = trimToNull(request.getDeviceId());
        String platform = trimToNull(request.getPlatform());

        // 🔥 유저 기준 기존 토큰 비활성화
        tokenRepository.deactivateAllByUserId(userId);

        // 🔥 token unique 기반 업서트
        tokenRepository.upsertByToken(userId, token, deviceId, platform);

        log.info("[FCM] token registered. userId={}, tokenPrefix={}",
                userId, token.substring(0, Math.min(12, token.length())));
    }

    private String normalize(String token) {
        if (token == null || token.trim().isEmpty()) {
            throw new IllegalArgumentException("FCM token is required");
        }
        return token.trim();
    }

    private String trimToNull(String v) {
        if (v == null) return null;
        String t = v.trim();
        return t.isEmpty() ? null : t;
    }

    private void validateNotJwtLike(String token) {
        long dots = token.chars().filter(c -> c == '.').count();
        if (token.startsWith("eyJ") && dots == 2) {
            throw new IllegalArgumentException("FCM token looks like JWT");
        }
    }
}
