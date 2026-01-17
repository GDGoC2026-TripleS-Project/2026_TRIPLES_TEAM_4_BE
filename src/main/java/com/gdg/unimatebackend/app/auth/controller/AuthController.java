package com.gdg.unimatebackend.app.auth.controller;

import com.gdg.unimatebackend.app.auth.dto.*;
import com.gdg.unimatebackend.app.auth.service.AuthService;
import com.gdg.unimatebackend.app.auth.service.KakaoApiService;
import com.gdg.unimatebackend.app.auth.service.NaverApiService;
import com.gdg.unimatebackend.app.auth.service.SocialAuthService;
import com.gdg.unimatebackend.app.user.entity.AuthProvider;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.util.UriComponentsBuilder;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
@Tag(name = "인증", description = "인증 관련 API")
public class AuthController {

    private final AuthService authService;
    private final SocialAuthService socialAuthService;
    private final NaverApiService naverApiService;
    private final KakaoApiService kakaoApiService;

    @Value("${oauth.naver.client-id}")
    private String naverClientId;

    @Value("${oauth.naver.redirect-uri}")
    private String naverRedirectUri;

    @Value("${oauth.naver.authorize-uri}")
    private String naverAuthorizeUri;

    // ===== KAKAO =====
    @Value("${oauth.kakao.client-id}")
    private String kakaoClientId;

    @Value("${oauth.kakao.redirect-uri}")
    private String kakaoRedirectUri;

    @Value("${oauth.kakao.authorize-uri}")
    private String kakaoAuthorizeUri;

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

    @PostMapping("/social/login")
    @Operation(summary = "소셜 로그인", description = "accessToken 기반 소셜 로그인을 처리합니다")
    public ResponseEntity<AuthResponse> socialLogin(@Valid @RequestBody SocialLoginRequest request) {
        AuthResponse response = socialAuthService.socialLogin(
                request.getProvider(),
                request.getAccessToken(),
                request.getEmail(),
                request.getNickname()
        );
        return ResponseEntity.ok(response);
    }

    @PostMapping("/logout")
    @Operation(summary = "로그아웃", description = "현재 로그인한 사용자를 로그아웃합니다")
    public ResponseEntity<Map<String, String>> logout(Authentication authentication) {
        Map<String, String> response = new HashMap<>();
        response.put("message", "로그아웃되었습니다");
        return ResponseEntity.ok(response);
    }

    @GetMapping("/naver/authorize-url")
    public ResponseEntity<Map<String, String>> naverAuthorizeUrl() {
        String state = UUID.randomUUID().toString(); // MVP: 서버 저장/검증은 생략
        String url = UriComponentsBuilder
                .fromUriString(naverAuthorizeUri)
                .queryParam("response_type", "code")
                .queryParam("client_id", naverClientId)
                .queryParam("redirect_uri", naverRedirectUri)
                .queryParam("state", state)
                .toUriString();

        Map<String, String> res = new HashMap<>();
        res.put("authorizeUrl", url);
        res.put("state", state);
        return ResponseEntity.ok(res);
    }

    @GetMapping("/naver/callback")
    public ResponseEntity<AuthResponse> naverCallback(
            @RequestParam String code,
            @RequestParam String state
    ) {
        var tokenRes = naverApiService.exchangeCodeToToken(code, state, naverRedirectUri);
        if (tokenRes == null || tokenRes.accessToken() == null) {
            throw new IllegalArgumentException("네이버 토큰 교환 실패: " +
                    (tokenRes == null ? "null" : tokenRes.errorDescription()));
        }

        AuthResponse response = socialAuthService.socialLogin(
                AuthProvider.NAVER,
                tokenRes.accessToken(),
                null,
                null
        );
        return ResponseEntity.ok(response);
    }

    @GetMapping("/kakao/authorize-url")
    public ResponseEntity<Map<String, String>> kakaoAuthorizeUrl() {
        String state = UUID.randomUUID().toString();

        String url = UriComponentsBuilder
                .fromUriString(kakaoAuthorizeUri)
                .queryParam("response_type", "code")
                .queryParam("client_id", kakaoClientId)
                .queryParam("redirect_uri", kakaoRedirectUri)
                .queryParam("state", state)
                .toUriString();

        Map<String, String> res = new HashMap<>();
        res.put("authorizeUrl", url);
        res.put("state", state);
        return ResponseEntity.ok(res);
    }

    @GetMapping("/kakao/callback")
    public ResponseEntity<AuthResponse> kakaoCallback(
            @RequestParam String code,
            @RequestParam(required = false) String state
    ) {
        // 1) code -> kakao access_token
        KakaoApiService.KakaoTokenResponse tokenRes = kakaoApiService.exchangeCodeToToken(code, kakaoRedirectUri);

        if (tokenRes == null || tokenRes.getAccessToken() == null || tokenRes.getAccessToken().isBlank()) {
            String reason = (tokenRes == null) ? "null" : (tokenRes.getErrorDescription() != null ? tokenRes.getErrorDescription() : tokenRes.getError());
            throw new IllegalArgumentException("카카오 토큰 교환 실패: " + reason);
        }

        // 2) kakao access_token -> 우리 JWT 발급
        AuthResponse response = socialAuthService.socialLogin(
                AuthProvider.KAKAO,
                tokenRes.getAccessToken(),
                null,
                null
        );
        return ResponseEntity.ok(response);
    }
}
