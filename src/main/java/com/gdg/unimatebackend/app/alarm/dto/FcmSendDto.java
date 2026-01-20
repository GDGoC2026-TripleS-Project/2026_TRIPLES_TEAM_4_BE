// ✅ FcmSendDto.java (풀버전)
// - 서버 단독 테스트에서도 Validation이 의미있게 동작하도록 @NotBlank 추가

package com.gdg.unimatebackend.app.alarm.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.*;

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

    @Builder
    public FcmSendDto(String token, String title, String body) {
        this.token = token;
        this.title = title;
        this.body = body;
    }
}
