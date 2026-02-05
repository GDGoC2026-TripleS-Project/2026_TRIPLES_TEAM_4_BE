package com.gdg.unimatebackend.alarm.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;

@Getter
public class FcmTokenRegisterRequest {

    @NotBlank
    private String token;

    // 프론트에서 넣어주면 정책이 완벽히 동작함 (권장)
    private String deviceId;
    private String platform;   // 예: ANDROID
}
