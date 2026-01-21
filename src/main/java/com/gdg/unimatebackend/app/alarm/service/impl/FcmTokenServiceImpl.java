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

        // 1) JWT 같은 쓰레기 값 차단
        validateNotJwtLike(token);

        String deviceId = trimToNull(request.getDeviceId());
        String platform = trimToNull(request.getPlatform());

        // 2) (정책 선택) "유저당 활성 1개"를 강하게 유지하고 싶으면
        //    기존 userId 토큰을 일단 비활성화하고 -> 이번 토큰을 활성 업서트
        //    프론트가 중복호출해도 결과는 항상 동일(마지막 토큰 1개 active)
        tokenRepository.deactivateAllByUserId(userId);

        // 3) 핵심: token unique 기반 업서트 (동시성 안전)
        tokenRepository.upsertByToken(userId, token, deviceId, platform);

        log.info("[FCM] token upserted. userId={}, deviceId={}, platform={}, tokenPrefix={}",
                userId, deviceId, platform, safePrefix(token));
    }

    private String normalizeToken(String token) {
        if (token == null) throw new IllegalArgumentException("token is required");
        String t = token.trim();
        if (t.isEmpty()) throw new IllegalArgumentException("token is required");
        return t;
    }

    private String trimToNull(String v) {
        if (v == null) return null;
        String t = v.trim();
        return t.isEmpty() ? null : t;
    }

    private void validateNotJwtLike(String token) {
        // JWT는 보통 "xxx.yyy.zzz" + "eyJ" 시작
        long dotCount = token.chars().filter(ch -> ch == '.').count();
        boolean looksLikeJwt = token.startsWith("eyJ") && dotCount == 2;
        if (looksLikeJwt) {
            throw new IllegalArgumentException("FCM token is invalid (looks like JWT).");
        }
    }

    private String safePrefix(String token) {
        int n = Math.min(token.length(), 12);
        return token.substring(0, n) + "...";
    }
}
