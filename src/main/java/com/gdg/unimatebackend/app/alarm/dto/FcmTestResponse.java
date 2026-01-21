package com.gdg.unimatebackend.app.alarm.dto;

import lombok.*;

@Getter @Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class FcmTestResponse {
    private boolean success;
    private String message;
    private String detail;
}
