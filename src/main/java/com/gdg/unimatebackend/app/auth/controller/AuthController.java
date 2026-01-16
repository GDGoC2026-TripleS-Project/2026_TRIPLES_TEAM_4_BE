package com.gdg.unimatebackend.app.auth.controller;

import com.gdg.unimatebackend.app.auth.dto.*;
import com.gdg.unimatebackend.app.auth.service.AuthService;
import com.gdg.unimatebackend.app.auth.service.SocialAuthService;
import com.gdg.unimatebackend.app.user.entity.AuthProvider;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
@Tag(name = "인증", description = "인증 관련 API")
public class AuthController {

    private final AuthService authService;
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

    @PostMapping("/social/login")
    @Operation(summary = "소셜 로그인", description = "카카오 로그인을 처리합니다")
    public ResponseEntity<AuthResponse> socialLogin(@Valid @RequestBody SocialLoginRequest request) {
        AuthResponse response = socialAuthService.socialLogin(
                request.getProvider(),
                request.getAccessToken(),
                request.getEmail(),
                request.getNickname()
        );
        return ResponseEntity.ok(response);
    }

    @PostMapping("/account/link")
    @Operation(summary = "계정 통합", description = "기존 계정에 다른 로그인 방식을 통합합니다")
    public ResponseEntity<AuthResponse> linkAccount(@Valid @RequestBody LinkAccountRequest request) {
        AuthResponse response;

        if (request.getExistingProvider() == AuthProvider.KAKAO) {
            // 소셜 로그인 계정에 이메일 로그인 추가
            response = authService.linkAccount(request);
        } else if (request.getExistingProvider() == AuthProvider.EMAIL) {
            // 이메일 계정에 소셜 로그인 추가는 소셜 로그인 시 처리
            // 여기서는 카카오 providerId를 받아서 처리
            throw new IllegalArgumentException("이메일 계정 통합은 소셜 로그인 시 처리됩니다");
        } else {
            throw new IllegalArgumentException("지원하지 않는 로그인 제공자입니다");
        }

        return ResponseEntity.ok(response);
    }

    @PostMapping("/account/link/social")
    @Operation(summary = "이메일 계정에 소셜 로그인 연동", description = "기존 이메일 계정에 소셜 로그인을 연동합니다")
    public ResponseEntity<AuthResponse> linkSocialAccount(
            @RequestParam String email,
            @RequestParam AuthProvider provider,
            @RequestParam String providerId) {
        AuthResponse response = socialAuthService.linkSocialAccount(email, provider, providerId);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/logout")
    @Operation(summary = "로그아웃", description = "현재 로그인한 사용자를 로그아웃합니다")
    public ResponseEntity<Map<String, String>> logout(Authentication authentication) {
        // JWT는 stateless이므로 클라이언트에서 토큰을 삭제하면 됩니다.
        // 필요시 토큰 블랙리스트 기능을 추가할 수 있습니다.
        Map<String, String> response = new HashMap<>();
        response.put("message", "로그아웃되었습니다");
        return ResponseEntity.ok(response);
    }
}

