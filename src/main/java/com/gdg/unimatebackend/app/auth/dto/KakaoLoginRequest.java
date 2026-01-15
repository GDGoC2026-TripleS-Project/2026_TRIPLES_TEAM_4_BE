package com.gdg.unimatebackend.app.auth.dto;

import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
public class KakaoLoginRequest {
    // 둘 중 하나만 오면 됨
    private String code;        // Authorization Code 방식
    private String accessToken; // 프론트가 직접 받은 토큰 방식

    // code 방식일 때 redirectUri를 요청에서 받는 옵션 (없으면 yml 값 사용)
    private String redirectUri;
}
