package com.gdg.unimatebackend.app.alarm.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class FcmSendRequest {
    private String title;
    private String body;
}
