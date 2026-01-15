package com.gdg.unimatebackend.app.auth.controller;

import com.gdg.unimatebackend.app.auth.dto.AuthTokenResponse;
import com.gdg.unimatebackend.app.auth.dto.KakaoLoginRequest;
import com.gdg.unimatebackend.app.auth.service.SocialAuthService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/auth/kakao")
public class AuthController {

    private final SocialAuthService socialAuthService;

    /**
     * 1) 카카오 로그인 페이지로 보낼 authorize URL 생성
     * - Postman에서 호출해서 URL을 받은 뒤 브라우저로 열어도 되고
     * - 브라우저에서 바로 /api/auth/kakao/authorize-url 을 열어도 됨
     */
    @GetMapping("/authorize-url")
    public ResponseEntity<String> authorizeUrl() {
        return ResponseEntity.ok(socialAuthService.buildKakaoAuthorizeUrl());
    }

    /**
     * 2) 카카오 로그인 후 리다이렉트되는 콜백
     * - redirect-uri로 등록한 주소가 여기와 정확히 같아야 함
     * - code -> token -> user/me -> 우리 서비스 JWT 발급
     */
    @GetMapping("/callback")
    public ResponseEntity<AuthTokenResponse> callback(@RequestParam("code") String code) {
        return ResponseEntity.ok(socialAuthService.kakaoLoginByCode(code));
    }

    /**
     * 3) (백엔드 단독 테스트용) access_token을 직접 넣어서 로그인 처리
     * - 프론트 없이 Postman으로 가장 쉽게 검증 가능
     */
    @PostMapping("/login")
    public ResponseEntity<AuthTokenResponse> login(@RequestBody KakaoLoginRequest request) {
        return ResponseEntity.ok(socialAuthService.kakaoLogin(request));
    }

    /**
     * ✅ (테스트 전용) 카카오 인가코드(code)만 화면에 출력
     * - 이 엔드포인트는 code를 "소비하지 않는다"
     * - Postman에서 /callback 호출할 때 code를 1회만 사용하게 해준다.
     */
    @GetMapping("/code")
    public ResponseEntity<String> codeOnly(@RequestParam("code") String code) {
        return ResponseEntity.ok(code);
    }
}
