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

@Tag(
        name = "카카오 인증",
        description = "카카오 OAuth 로그인 및 JWT 발급 API"
)
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/auth/kakao")
public class AuthController {

    private final SocialAuthService socialAuthService;

    @Operation(
            summary = "카카오 로그인 Authorize URL 생성",
            description = """
                    카카오 로그인 페이지로 이동하기 위한 authorize URL을 생성합니다.

                    ✔️ 사용 방법
                    - 이 API를 호출해 URL을 받습니다.
                    - 받은 URL을 브라우저에서 열면 카카오 로그인 화면으로 이동합니다.
                    - 로그인 성공 시 redirect-uri로 code가 전달됩니다.
                    """
    )
    @GetMapping("/authorize-url")
    public ResponseEntity<String> authorizeUrl() {
        return ResponseEntity.ok(socialAuthService.buildKakaoAuthorizeUrl());
    }

    @Operation(
            summary = "카카오 로그인 콜백",
            description = """
                    카카오 로그인 성공 후 redirect-uri로 전달되는 code를 처리합니다.

                    처리 흐름:
                    1️⃣ code → 카카오 access_token 요청
                    2️⃣ access_token → 카카오 사용자 정보 조회
                    3️⃣ 우리 서비스 JWT 발급

                    ⚠️ code는 1회용입니다.
                    """
    )
    @GetMapping("/callback")
    public ResponseEntity<AuthTokenResponse> callback(
            @Parameter(
                    description = "카카오 OAuth 인가 코드 (1회용)",
                    required = true,
                    example = "QkRjS1l..."
            )
            @RequestParam("code") String code
    ) {
        return ResponseEntity.ok(socialAuthService.kakaoLoginByCode(code));
    }

    @Operation(
            summary = "카카오 access_token 직접 로그인 (백엔드 테스트용)",
            description = """
                    프론트엔드 없이 Postman 등으로 테스트할 때 사용하는 API입니다.

                    ✔️ 카카오 access_token을 직접 전달하면
                    → 사용자 조회
                    → 우리 서비스 JWT 발급을 수행합니다.
                    """
    )
    @PostMapping("/login")
    public ResponseEntity<AuthTokenResponse> login(
            @RequestBody KakaoLoginRequest request
    ) {
        return ResponseEntity.ok(socialAuthService.kakaoLogin(request));
    }

    @Operation(
            summary = "카카오 인가 코드(code) 확인용",
            description = """
                    redirect-uri 테스트 전용 API입니다.

                    ✔️ 전달된 code를 소비하지 않고 그대로 반환합니다.
                    ✔️ 실제 로그인(callback) 전에 code 값을 눈으로 확인할 때 사용합니다.
                    """
    )
    @GetMapping("/code")
    public ResponseEntity<String> codeOnly(
            @Parameter(
                    description = "카카오 OAuth 인가 코드",
                    required = true
            )
            @RequestParam("code") String code
    ) {
        return ResponseEntity.ok(code);
    }
}
