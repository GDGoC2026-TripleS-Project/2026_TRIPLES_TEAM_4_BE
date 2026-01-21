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

        FcmDeviceToken entity = tokenRepository.findByToken(token)
                .orElseGet(() -> FcmDeviceToken.builder()
                        .token(token)
                        .isActive(true)
                        .build());

        entity.activateForUser(
                userId
        );

        tokenRepository.save(entity);

        log.info("[FCM] token registered. userId={}, deviceId={}, platform={}",
                userId);
    }
}
