package com.gdg.unimatebackend.app.auth.controller;

import com.gdg.unimatebackend.app.auth.dto.AuthTokenResponse;
import com.gdg.unimatebackend.app.auth.dto.KakaoLoginRequest;
import com.gdg.unimatebackend.app.auth.service.SocialAuthService;
import com.gdg.unimatebackend.global.security.jwt.JwtUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final SocialAuthService socialAuthService;
    private final JwtUtil jwtUtil;

    @Value("${app.dev-token.enabled:false}")
    private boolean devTokenEnabled;

    // ✅ 카카오 로그인 (운영에서도 사용)
    @PostMapping("/kakao")
    public AuthTokenResponse kakaoLogin(@RequestBody KakaoLoginRequest request) {
        return socialAuthService.kakaoLogin(request);
    }

    // ⚠️ 개발용 토큰 발급 (운영에서는 app.dev-token.enabled=false로 막힘)
    @GetMapping("/dev-token")
    public Map<String, Object> devToken(@RequestParam Long userId) {
        if (!devTokenEnabled) {
            // 운영에서 404처럼 보이게 하고 싶으면 IllegalArgumentException 대신 RuntimeException으로 커스텀 처리도 가능
            throw new IllegalArgumentException("dev-token is disabled");
        }
        String token = jwtUtil.generateToken(userId);
        return Map.of("token", token, "userId", userId);
    }
}
