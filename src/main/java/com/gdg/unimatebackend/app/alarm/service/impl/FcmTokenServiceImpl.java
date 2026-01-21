package com.gdg.unimatebackend.app.alarm.service.impl;

import com.gdg.unimatebackend.app.alarm.dto.FcmTokenRegisterRequest;
import com.gdg.unimatebackend.app.alarm.entity.FcmDeviceToken;
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
        String token = request.getToken().trim();

        // 1) JWT가 들어오는 사고를 백엔드에서 차단
        validateNotJwtLike(token);

        // 2) (선택) 어떤 유저가 이미 이 토큰을 쓰고 있으면 "현재 유저"로 재할당
        tokenRepository.findByToken(token).ifPresent(existingByToken -> {
            if (existingByToken.getUserId() != null && !existingByToken.getUserId().equals(userId)) {
                // 정책상 user당 1개니까: 이전 유저 토큰을 비활성화(or 삭제)하고 재할당하는 편이 안전
                // 여기서는 "재할당" 선택
            }
        });

        // 3) 유저 기준으로 1개만 유지: 있으면 update, 없으면 create
        FcmDeviceToken entity = tokenRepository.findByUserId(userId)
                .orElseGet(() -> FcmDeviceToken.builder()
                        .userId(userId)
                        .isActive(true)
                        .build());

        entity.activateForUser(userId, request.getDeviceId(), request.getPlatform());
        entity.updateToken(token); // 아래 엔티티에 메서드 추가 추천
        tokenRepository.save(entity);

        log.info("[FCM] token registered(1-per-user). userId={}, deviceId={}, platform={}",
                userId, request.getDeviceId(), request.getPlatform());
    }

    private void validateNotJwtLike(String token) {
        // JWT는 보통 "header.payload.signature" 형태(점 2개) + "eyJ"로 시작하는 경우가 많음
        boolean looksLikeJwt = token.startsWith("eyJ") && token.chars().filter(ch -> ch == '.').count() == 2;
        if (looksLikeJwt) {
            throw new IllegalArgumentException("FCM token is invalid (looks like JWT).");
        }
    }
}
