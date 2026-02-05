// ✅ FcmSendDto.java (풀버전)
// - 서버 단독 테스트에서도 Validation이 의미있게 동작하도록 @NotBlank 추가

package com.gdg.unimatebackend.alarm.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.*;

import java.util.Map;

@Getter
@ToString
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class FcmSendDto {

    @NotBlank
    private String token;

    @NotBlank
    private String title;

    @NotBlank
    private String body;

    private Map<String, String> data;

    @Builder
    public FcmSendDto(String token, String title, String body, Map<String, String> data) {
        this.token = token;
        this.title = title;
        this.body = body;
        this.data = data;
    }
}
