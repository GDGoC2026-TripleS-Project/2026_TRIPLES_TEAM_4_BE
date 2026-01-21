package com.gdg.unimatebackend.app.alarm.dto;

import lombok.Getter;

@Getter
public class FcmSendRequest {
    // 없으면 기본 템플릿으로 발송
    private String title;
    private String body;
}
