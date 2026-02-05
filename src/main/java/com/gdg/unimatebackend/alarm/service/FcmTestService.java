package com.gdg.unimatebackend.alarm.service;

import com.gdg.unimatebackend.alarm.dto.FcmSendRequest;
import com.gdg.unimatebackend.alarm.dto.FcmTestResponse;
import org.springframework.security.core.Authentication;

public interface FcmTestService {
    Long extractUserId(Authentication authentication);

    // request를 받아서 title/body 커스텀
    FcmTestResponse sendTestToUser(Long userId, FcmSendRequest request);
}
