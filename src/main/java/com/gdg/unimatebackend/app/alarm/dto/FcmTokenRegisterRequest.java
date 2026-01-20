package com.gdg.unimatebackend.app.alarm.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;

@Getter
public class FcmTokenRegisterRequest {

    @NotBlank
    private String token;

    // 선택: 기기 식별자(에뮬레이터/실기기 구분용)
    private String deviceId;

    // 선택: ANDROID
    private String platform;
}
