package com.gdg.unimatebackend.app.auth.controller;

import com.gdg.unimatebackend.app.auth.dto.AuthTokenResponse;
import com.gdg.unimatebackend.app.auth.dto.KakaoLoginRequest;
import com.gdg.unimatebackend.app.auth.service.SocialAuthService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/auth")
@Tag(name = "인증", description = "인증 관련 API")
public class AuthController {

    private final SocialAuthService socialAuthService;

    @GetMapping("/kakao/authorize-url")
    @Operation(
            summary = "카카오 Authorize URL 생성",
            description = """
                    카카오 로그인 화면으로 이동할 Authorize URL을 생성합니다.
                    - client_id는 REST API 키를 사용해야 합니다.
                    - redirect_uri는 카카오 콘솔에 등록된 값과 정확히 동일해야 합니다.
                    """
    )
    public ResponseEntity<Map<String, String>> kakaoAuthorizeUrl() {
        String url = socialAuthService.buildKakaoAuthorizeUrl();
        return ResponseEntity.ok(Map.of("authorizeUrl", url));
    }

    @GetMapping("/kakao/callback")
    @Operation(
            summary = "카카오 OAuth 콜백(인가코드 처리)",
            description = """
                    카카오 로그인 성공 후 redirect_uri로 호출되는 콜백입니다.
                    - code(인가코드)를 받아 토큰 교환 -> 사용자 정보 조회 -> 우리 서비스 JWT 발급까지 수행합니다.
                    """
    )
    public ResponseEntity<AuthTokenResponse> kakaoCallback(
            @Parameter(description = "카카오에서 전달되는 인가코드(code)", required = true, example = "abc123...")
            @RequestParam("code") String code
    ) {
        return ResponseEntity.ok(socialAuthService.kakaoLoginByCode(code));
    }

    @PostMapping("/kakao/login")
    @Operation(
            summary = "카카오 access_token 로그인(테스트/내부용)",
            description = """
                    프론트 없이 빠르게 검증하기 위한 엔드포인트입니다.
                    - 카카오 access_token을 직접 받아 사용자 정보 조회 -> 우리 서비스 JWT 발급을 수행합니다.
                    """
    )
    public ResponseEntity<AuthTokenResponse> kakaoLogin(@RequestBody KakaoLoginRequest request) {
        return ResponseEntity.ok(socialAuthService.kakaoLogin(request));
    }
}
