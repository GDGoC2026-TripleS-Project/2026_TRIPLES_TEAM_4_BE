package com.gdg.unimatebackend.app.alarm.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;

@Getter
public class FcmTokenRegisterRequest {

    @NotBlank
    private String token;
}
