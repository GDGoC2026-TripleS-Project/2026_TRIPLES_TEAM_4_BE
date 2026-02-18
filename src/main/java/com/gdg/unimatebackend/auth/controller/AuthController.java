package com.gdg.unimatebackend.auth.controller;

import com.gdg.unimatebackend.auth.dto.*;
import com.gdg.unimatebackend.auth.service.*;
import com.gdg.unimatebackend.user.entity.AuthProvider;
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
    public ResponseEntity<Map<String, String>> sendVerificationCode(@Valid @RequestBody EmailVerificationRequest request) {
        authService.sendVerificationCode(request.getEmail());
        return ResponseEntity.ok(Map.of("message", "인증 코드가 발송되었습니다"));
    }

    @PostMapping("/email/verification/confirm")
    @Operation(summary = "이메일 인증번호 확인", description = "이메일 인증번호를 확인합니다")
    public ResponseEntity<Map<String, String>> confirmVerificationCode(@Valid @RequestBody EmailVerificationConfirmRequest request) {
        authService.verifyEmailCode(request.getEmail(), request.getCode());
        return ResponseEntity.ok(Map.of("message", "이메일 인증이 완료되었습니다"));
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
        return ResponseEntity.ok(authService.login(request));
    }

    @DeleteMapping("/account")
    @Operation(summary = "회원탈퇴", description = "현재 로그인한 사용자의 계정을 탈퇴(삭제)합니다")
    public ResponseEntity<Map<String, String>> deleteAccount(Authentication authentication) {
        if (authentication == null || authentication.getPrincipal() == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("message", "JWT가 필요합니다"));
        }
        Long userId = (Long) authentication.getPrincipal();
        authService.deleteAccount(userId);
        return ResponseEntity.ok(Map.of("message", "회원탈퇴가 완료되었습니다"));
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
    public ResponseEntity<Map<String, String>> changePassword(Authentication authentication,
                                                              @Valid @RequestBody ChangePasswordRequest request) {
        if (authentication == null || authentication.getPrincipal() == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("message", "JWT가 필요합니다"));
        }
        Long userId = (Long) authentication.getPrincipal();
        authService.changePassword(userId, request);
        return ResponseEntity.ok(Map.of("message", "비밀번호가 변경되었습니다"));
    }

    @PostMapping("/password/reset")
    @Operation(summary = "비밀번호 재설정", description = "이메일 인증을 통해 비밀번호를 재설정합니다")
    public ResponseEntity<Map<String, String>> resetPassword(@Valid @RequestBody ResetPasswordRequest request) {
        authService.resetPassword(request);
        return ResponseEntity.ok(Map.of("message", "비밀번호가 재설정되었습니다"));
    }

    @GetMapping("/nickname/check")
    @Operation(summary = "닉네임 중복 확인", description = "닉네임의 중복 여부를 확인합니다")
    public ResponseEntity<Map<String, String>> checkNickname(@RequestParam String nickname) {
        authService.checkNicknameDuplicate(nickname);
        return ResponseEntity.ok(Map.of("message", "사용 가능한 닉네임입니다"));
    }

    @PostMapping("/logout")
    @Operation(summary = "로그아웃", description = "현재 로그인한 사용자를 로그아웃합니다 (클라이언트 토큰 삭제 방식)")
    public ResponseEntity<Map<String, String>> logout(Authentication authentication) {
        if (authentication == null || authentication.getPrincipal() == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("message", "JWT가 필요합니다"));
        }
        Long userId = (Long) authentication.getPrincipal();
        authService.logout(userId);
        return ResponseEntity.ok(Map.of("message", "로그아웃되었습니다"));
    }

    @PostMapping("/refresh")
    @Operation(summary = "토큰 재발급", description = "refresh token으로 access token을 재발급합니다")
    public ResponseEntity<AuthResponse> refresh(@Valid @RequestBody RefreshTokenRequest request) {
        return ResponseEntity.ok(authService.refresh(request.getRefreshToken()));
    }

    // 1) 로그인 시작
    @GetMapping("/naver/authorize-url")
    @Operation(summary = "[네이버] 로그인 시작 URL 조회")
    public ResponseEntity<AuthorizeUrlResponse> naverAuthorizeUrl() {
        String state = UUID.randomUUID().toString();
        return ResponseEntity.ok(new AuthorizeUrlResponse(naverClient.buildAuthorizeUrl(state), state));
    }

    @GetMapping("/kakao/authorize-url")
    @Operation(summary = "[카카오] 로그인 시작 URL 조회")
    public ResponseEntity<AuthorizeUrlResponse> kakaoAuthorizeUrl() {
        String state = UUID.randomUUID().toString();
        return ResponseEntity.ok(new AuthorizeUrlResponse(kakaoClient.buildAuthorizeUrl(state), state));
    }

    // 2) 콜백
    @GetMapping("/naver/callback")
    @Operation(summary = "[네이버] OAuth 콜백")
    public ResponseEntity<AuthResponse> naverCallback(
            @Parameter(description = "네이버 인가 코드", required = true) @RequestParam String code,
            @Parameter(description = "CSRF 방지를 위한 state 값", required = true) @RequestParam String state
    ) {
        var token = naverClient.exchangeCodeForToken(code, state);
        if (token == null || token.accessToken() == null || token.accessToken().isBlank()) {
            throw new IllegalArgumentException("NAVER_TOKEN_EXCHANGE_FAILED");
        }
        return ResponseEntity.ok(socialAuthService.loginByAccessToken(AuthProvider.NAVER, token.accessToken()));
    }

    @GetMapping("/kakao/callback")
    @Operation(summary = "[카카오] OAuth 콜백")
    public ResponseEntity<AuthResponse> kakaoCallback(
            @Parameter(description = "카카오 인가 코드", required = true) @RequestParam String code
    ) {
        var token = kakaoClient.exchangeCodeForToken(code);
        if (token == null || token.getAccessToken() == null || token.getAccessToken().isBlank()) {
            throw new IllegalArgumentException("KAKAO_TOKEN_EXCHANGE_FAILED");
        }
        return ResponseEntity.ok(socialAuthService.loginByAccessToken(AuthProvider.KAKAO, token.getAccessToken()));
    }

    // 3) accessToken 직접 전달 (선택)
    @PostMapping("/social/login")
    @Operation(summary = "[공통] accessToken 기반 소셜 로그인")
    public ResponseEntity<AuthResponse> socialLogin(@Valid @RequestBody SocialLoginRequest request) {
        return ResponseEntity.ok(socialAuthService.loginByAccessToken(request.getProvider(), request.getAccessToken()));
    }
}
