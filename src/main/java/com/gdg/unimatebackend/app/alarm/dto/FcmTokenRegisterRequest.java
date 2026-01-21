package com.gdg.unimatebackend.app.alarm.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;

@Getter
public class FcmTokenRegisterRequest {

    @NotBlank
    private String token;

    // 선택: 디바이스 식별/플랫폼(운영에서 유용)
    private String deviceId;
    private String platform;
}
