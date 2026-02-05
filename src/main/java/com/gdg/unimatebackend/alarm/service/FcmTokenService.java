package com.gdg.unimatebackend.alarm.service;

import com.gdg.unimatebackend.alarm.dto.FcmTokenRegisterRequest;

public interface FcmTokenService {
    void register(Long userId, FcmTokenRegisterRequest request);
}
