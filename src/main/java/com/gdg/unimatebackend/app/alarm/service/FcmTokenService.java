package com.gdg.unimatebackend.app.alarm.service;

import com.gdg.unimatebackend.app.alarm.dto.FcmTokenRegisterRequest;

public interface FcmTokenService {
    void register(Long userId, FcmTokenRegisterRequest request);
}
