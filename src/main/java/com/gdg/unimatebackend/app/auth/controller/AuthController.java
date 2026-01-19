package com.gdg.unimatebackend.app.auth.controller;

import com.gdg.unimatebackend.app.auth.dto.*;
import com.gdg.unimatebackend.app.auth.service.*;
import com.gdg.unimatebackend.app.auth.service.KakaoApiService;
import com.gdg.unimatebackend.app.auth.service.NaverApiService;
import com.gdg.unimatebackend.app.user.entity.AuthProvider;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
@Tag(name = "OAuth", description = "네이버/카카오 OAuth")
public class AuthController {

    private final AuthService authService;
    private final NaverApiService naverClient;
    private final KakaoApiService kakaoClient;
    private final SocialAuthService socialAuthService;


    @PostMapping("/email/verification/send")
    @Operation(summary = "이메일 인증번호 전송", description = "이메일로 인증번호를 전송합니다")
    public ResponseEntity<Map<String, String>> sendVerificationCode(
            @Valid @RequestBody EmailVerificationRequest request) {
        authService.sendVerificationCode(request.getEmail());
        Map<String, String> response = new HashMap<>();
        response.put("message", "인증 코드가 발송되었습니다");
        return ResponseEntity.ok(response);
    }

    @PostMapping("/email/verification/confirm")
    @Operation(summary = "이메일 인증번호 확인", description = "이메일 인증번호를 확인합니다")
    public ResponseEntity<Map<String, String>> confirmVerificationCode(
            @Valid @RequestBody EmailVerificationConfirmRequest request) {
        authService.verifyEmailCode(request.getEmail(), request.getCode());
        Map<String, String> response = new HashMap<>();
        response.put("message", "이메일 인증이 완료되었습니다");
        return ResponseEntity.ok(response);
    }

    @PostMapping("/signup")
    @Operation(summary = "회원가입", description = "새로운 사용자를 등록합니다")
    public ResponseEntity<AuthResponse> signup(@Valid @RequestBody SignupRequest request) {
        AuthResponse response = authService.signup(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @PostMapping("/login")
    @Operation(summary = "로그인", description = "이메일과 비밀번호로 로그인합니다")
    public ResponseEntity<AuthResponse> login(@Valid @RequestBody LoginRequest request) {
        AuthResponse response = authService.login(request);
        return ResponseEntity.ok(response);
    }

    @DeleteMapping("/account")
    @Operation(summary = "회원탈퇴", description = "현재 로그인한 사용자의 계정을 탈퇴합니다")
    public ResponseEntity<Map<String, String>> deleteAccount(Authentication authentication) {
        Long userId = (Long) authentication.getPrincipal();
        authService.deleteAccount(userId);
        Map<String, String> response = new HashMap<>();
        response.put("message", "회원탈퇴가 완료되었습니다");
        return ResponseEntity.ok(response);
    }

    @PostMapping("/email/find")
    @Operation(summary = "이메일 찾기", description = "인증 코드를 통해 이메일을 찾습니다")
    public ResponseEntity<Map<String, String>> findEmail(@Valid @RequestBody FindEmailRequest request) {
        String email = authService.findEmail(request);
        Map<String, String> response = new HashMap<>();
        response.put("email", email);
        response.put("message", "이메일을 찾았습니다");
        return ResponseEntity.ok(response);
    }

    @PostMapping("/password/change")
    @Operation(summary = "비밀번호 변경", description = "현재 로그인한 사용자의 비밀번호를 변경합니다")
    public ResponseEntity<Map<String, String>> changePassword(
            Authentication authentication,
            @Valid @RequestBody ChangePasswordRequest request) {
        Long userId = (Long) authentication.getPrincipal();
        authService.changePassword(userId, request);
        Map<String, String> response = new HashMap<>();
        response.put("message", "비밀번호가 변경되었습니다");
        return ResponseEntity.ok(response);
    }

    @PostMapping("/password/reset")
    @Operation(summary = "비밀번호 재설정", description = "이메일 인증을 통해 비밀번호를 재설정합니다")
    public ResponseEntity<Map<String, String>> resetPassword(@Valid @RequestBody ResetPasswordRequest request) {
        authService.resetPassword(request);
        Map<String, String> response = new HashMap<>();
        response.put("message", "비밀번호가 재설정되었습니다");
        return ResponseEntity.ok(response);
    }

    @GetMapping("/nickname/check")
    @Operation(summary = "닉네임 중복 확인", description = "닉네임의 중복 여부를 확인합니다")
    public ResponseEntity<Map<String, String>> checkNickname(@RequestParam String nickname) {
        authService.checkNicknameDuplicate(nickname);
        Map<String, String> response = new HashMap<>();
        response.put("message", "사용 가능한 닉네임입니다");
        return ResponseEntity.ok(response);
    }

    @PostMapping("/logout")
    @Operation(summary = "로그아웃", description = "현재 로그인한 사용자를 로그아웃합니다")
    public ResponseEntity<Map<String, String>> logout(Authentication authentication) {
        Map<String, String> response = new HashMap<>();
        response.put("message", "로그아웃되었습니다");
        return ResponseEntity.ok(response);
    }

    // =====================================================
    // 1. 로그인 시작 (프론트가 호출)
    // =====================================================

    @Operation(
            summary = "[네이버] 로그인 시작 URL 조회",
            description =
                    """
                    프론트에서 네이버 로그인 버튼 클릭 시 호출하는 API입니다.

                    응답으로 내려오는 authorizeUrl로 브라우저를 이동시키면
                    네이버 로그인 화면이 열립니다.

                    로그인 성공 후 네이버는
                    /api/auth/naver/callback 으로 code와 state를 전달합니다.
                    """
    )
    @GetMapping("/naver/authorize-url")
    public ResponseEntity<AuthorizeUrlResponse> naverAuthorizeUrl() {
        String state = UUID.randomUUID().toString();
        return ResponseEntity.ok(
                new AuthorizeUrlResponse(
                        naverClient.buildAuthorizeUrl(state),
                        state
                )
        );
    }

    @Operation(
            summary = "[카카오] 로그인 시작 URL 조회",
            description =
                    """
                    프론트에서 카카오 로그인 버튼 클릭 시 호출하는 API입니다.

                    응답으로 내려오는 authorizeUrl로 브라우저를 이동시키면
                    카카오 로그인 화면이 열립니다.

                    로그인 성공 후 카카오는
                    /api/auth/kakao/callback 으로 code를 전달합니다.
                    """
    )
    @GetMapping("/kakao/authorize-url")
    public ResponseEntity<AuthorizeUrlResponse> kakaoAuthorizeUrl() {
        String state = UUID.randomUUID().toString();
        return ResponseEntity.ok(
                new AuthorizeUrlResponse(
                        kakaoClient.buildAuthorizeUrl(state),
                        state
                )
        );
    }

    // =====================================================
    // 2. OAuth 콜백 (백엔드가 code를 직접 처리)
    // =====================================================

    @Operation(
            summary = "[네이버] OAuth 콜백",
            description =
                    """
                    네이버 로그인 완료 후 네이버가 호출하는 콜백 API입니다.

                    - code/state로 네이버 access token을 발급받습니다.
                    - 네이버 사용자 정보를 조회합니다.
                    - 우리 서비스 JWT를 발급하여 응답합니다.

                    프론트는 응답의 token 값을 저장한 뒤
                    이후 API 호출 시 Authorization: Bearer {token} 으로 사용하면 됩니다.
                    """
    )
    @GetMapping("/naver/callback")
    public ResponseEntity<AuthResponse> naverCallback(
            @Parameter(description = "네이버 인가 코드", required = true)
            @RequestParam String code,
            @Parameter(description = "CSRF 방지를 위한 state 값", required = true)
            @RequestParam String state
    ) {
        var token = naverClient.exchangeCodeForToken(code, state);

        if (token == null || token.accessToken() == null || token.accessToken().isBlank()) {
            throw new IllegalArgumentException("NAVER_TOKEN_EXCHANGE_FAILED");
        }

        return ResponseEntity.ok(
                socialAuthService.loginByAccessToken(
                        AuthProvider.NAVER,
                        token.accessToken()
                )
        );
    }

    @Operation(
            summary = "[카카오] OAuth 콜백",
            description =
                    """
                    카카오 로그인 완료 후 카카오가 호출하는 콜백 API입니다.

                    - code로 카카오 access token을 발급받습니다.
                    - 카카오 사용자 정보를 조회합니다.
                    - 우리 서비스 JWT를 발급하여 응답합니다.

                    프론트는 응답의 token 값을 저장한 뒤
                    이후 API 호출 시 Authorization: Bearer {token} 으로 사용하면 됩니다.
                    """
    )
    @GetMapping("/kakao/callback")
    public ResponseEntity<AuthResponse> kakaoCallback(
            @Parameter(description = "카카오 인가 코드", required = true)
            @RequestParam String code
    ) {
        var token = kakaoClient.exchangeCodeForToken(code);

        if (token == null || token.getAccessToken() == null || token.getAccessToken().isBlank()) {
            throw new IllegalArgumentException("KAKAO_TOKEN_EXCHANGE_FAILED");
        }

        return ResponseEntity.ok(
                socialAuthService.loginByAccessToken(
                        AuthProvider.KAKAO,
                        token.getAccessToken()
                )
        );
    }

    // =====================================================
    // 3. 프론트가 accessToken을 직접 전달하는 방식
    // =====================================================

    @Operation(
            summary = "[공통] accessToken 기반 소셜 로그인",
            description =
                    """
                    프론트가 이미 네이버/카카오 accessToken을 보유한 경우 사용하는 API입니다.

                    - provider: NAVER 또는 KAKAO
                    - accessToken: 각 소셜 서비스에서 발급받은 토큰

                    백엔드는 해당 토큰으로 사용자 정보를 조회한 뒤
                    우리 서비스 JWT를 발급합니다.
                    """
    )
    @PostMapping("/social/login")
    public ResponseEntity<AuthResponse> socialLogin(
            @Valid @RequestBody SocialLoginRequest request
    ) {
        return ResponseEntity.ok(
                socialAuthService.loginByAccessToken(
                        request.getProvider(),
                        request.getAccessToken()
                )
        );
    }
}
