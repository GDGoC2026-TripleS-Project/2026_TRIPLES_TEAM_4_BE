package com.gdg.unimatebackend.app.alarm.service;

import com.gdg.unimatebackend.app.alarm.dto.FcmTestResponse;
import org.springframework.security.core.Authentication;

public interface FcmTestService {
    Long extractUserId(Authentication authentication);
    FcmTestResponse sendTestToUser(Long userId);
}
