package com.gdg.unimatebackend.auth.service;

import com.gdg.unimatebackend.alarm.repository.FcmDeviceTokenRepository;
import com.gdg.unimatebackend.auth.dto.*;
import com.gdg.unimatebackend.user.entity.AuthProvider;
import com.gdg.unimatebackend.user.entity.EmailVerification;
import com.gdg.unimatebackend.user.entity.User;
import com.gdg.unimatebackend.user.repository.EmailVerificationRepository;
import com.gdg.unimatebackend.user.repository.UserRepository;
import com.gdg.unimatebackend.global.exception.AccountLinkRequiredException;
import com.gdg.unimatebackend.global.security.jwt.JwtUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository userRepository;
    private final EmailVerificationRepository emailVerificationRepository;
    private final EmailService emailService;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtil jwtUtil;
    private final FcmDeviceTokenRepository fcmDeviceTokenRepository;

    private static final int VERIFICATION_CODE_EXPIRY_MINUTES = 10;

    @Transactional
    public void sendVerificationCode(String email) {
        if (userRepository.existsByEmail(email)) {
            throw new IllegalArgumentException("이미 가입된 이메일입니다");
        }

        String code = emailService.generateVerificationCode();

        emailVerificationRepository.findTopByEmailOrderByCreatedAtDesc(email)
                .ifPresent(verification -> {
                    if (!verification.getVerified() && !verification.isExpired()) {
                        emailVerificationRepository.delete(verification);
                    }
                });

        EmailVerification verification = EmailVerification.builder()
                .email(email)
                .code(code)
                .verified(false)
                .expiresAt(LocalDateTime.now().plusMinutes(VERIFICATION_CODE_EXPIRY_MINUTES))
                .build();

        emailVerificationRepository.save(verification);
        emailService.sendVerificationEmail(email, code);
    }

    @Transactional
    public void verifyEmailCode(String email, String code) {
        Optional<EmailVerification> verificationOpt = emailVerificationRepository
                .findByEmailAndCodeAndVerifiedFalse(email, code);

        if (verificationOpt.isEmpty()) throw new IllegalArgumentException("인증 코드가 올바르지 않습니다");

        EmailVerification verification = verificationOpt.get();
        if (verification.isExpired()) throw new IllegalArgumentException("인증 코드가 만료되었습니다");

        verification.verify();
        emailVerificationRepository.save(verification);
    }

    @Transactional
    public AuthResponse signup(SignupRequest request) {
        Optional<User> existingUserOpt = userRepository.findByEmail(request.getEmail());

        if (existingUserOpt.isPresent()) {
            User existingUser = existingUserOpt.get();

            if (existingUser.getProvider() == AuthProvider.EMAIL) {
                throw new IllegalArgumentException("이미 가입된 이메일입니다");
            }

            if (existingUser.getPassword() == null) {
                throw new AccountLinkRequiredException(existingUser.getProvider(), request.getEmail());
            }

            throw new IllegalArgumentException("이미 이메일 로그인 비밀번호가 설정된 계정입니다");
        }

        if (userRepository.existsByNickname(request.getNickname())) {
            throw new IllegalArgumentException("이미 사용 중인 닉네임입니다");
        }

        Optional<EmailVerification> verificationOpt = emailVerificationRepository
                .findTopByEmailOrderByCreatedAtDesc(request.getEmail());

        if (verificationOpt.isEmpty() || !verificationOpt.get().getVerified()) {
            throw new IllegalArgumentException("이메일 인증을 완료해주세요");
        }

        User user = User.builder()
                .email(request.getEmail())
                .password(passwordEncoder.encode(request.getPassword()))
                .nickname(request.getNickname())
                .emailVerified(true)
                .provider(AuthProvider.EMAIL)
                .providerId(null)
                .active(true)
                .build();

        user = userRepository.save(user);

        String token = jwtUtil.generateToken(user.getId(), user.getEmail());

        return AuthResponse.builder()
                .token(token)
                .userId(user.getId())
                .email(user.getEmail())
                .nickname(user.getNickname())
                .build();
    }

    @Transactional
    public AuthResponse login(LoginRequest request) {
        User user = userRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new IllegalArgumentException("이메일 또는 비밀번호가 올바르지 않습니다"));

        if (!user.getActive()) throw new IllegalArgumentException("탈퇴한 계정입니다");
        if (user.getProvider() != AuthProvider.EMAIL) {
            throw new IllegalArgumentException("소셜 로그인으로 가입한 계정입니다. 소셜 로그인을 사용해주세요");
        }
        if (user.getPassword() == null || !passwordEncoder.matches(request.getPassword(), user.getPassword())) {
            throw new IllegalArgumentException("이메일 또는 비밀번호가 올바르지 않습니다");
        }

        String token = jwtUtil.generateToken(user.getId(), user.getEmail());

        return AuthResponse.builder()
                .token(token)
                .userId(user.getId())
                .email(user.getEmail())
                .nickname(user.getNickname())
                .build();
    }

    /**
     * ✅ 계정탈퇴: "전부 삭제" 시도
     * 1) 사용자 관련 데이터 선삭제 (현재 문서에 존재하는 것: fcm_device_token)
     * 2) users 삭제 시도
     * 3) FK 등으로 막히면 active=false로 폴백
     */
    @Transactional
    public void deleteAccount(Long userId) {
        // (1) FCM 토큰 삭제
        fcmDeviceTokenRepository.deleteByUserId(userId);

        // (2) users 삭제 시도
        try {
            userRepository.deleteById(userId);
            userRepository.flush(); // 즉시 반영해서 FK 문제를 여기서 터뜨리게
        } catch (DataIntegrityViolationException e) {
            // (3) FK로 막히면 폴백: soft delete
            User user = userRepository.findById(userId)
                    .orElseThrow(() -> new IllegalArgumentException("사용자를 찾을 수 없습니다"));
            user.deactivate();
            userRepository.save(user);
        }
    }

    @Transactional
    public String findEmail(FindEmailRequest request) {
        Optional<EmailVerification> verificationOpt = emailVerificationRepository
                .findByEmailAndCodeAndVerifiedFalse(request.getEmail(), request.getCode());

        if (verificationOpt.isEmpty()) throw new IllegalArgumentException("인증 코드가 올바르지 않습니다");

        EmailVerification verification = verificationOpt.get();
        if (verification.isExpired()) throw new IllegalArgumentException("인증 코드가 만료되었습니다");

        User user = userRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new IllegalArgumentException("가입된 이메일이 아닙니다"));

        return user.getEmail();
    }

    @Transactional
    public void changePassword(Long userId, ChangePasswordRequest request) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("사용자를 찾을 수 없습니다"));

        if (user.getPassword() == null) {
            throw new IllegalArgumentException("소셜 로그인 계정은 비밀번호 변경을 지원하지 않습니다");
        }

        if (!passwordEncoder.matches(request.getCurrentPassword(), user.getPassword())) {
            throw new IllegalArgumentException("현재 비밀번호가 올바르지 않습니다");
        }

        user.updatePassword(passwordEncoder.encode(request.getNewPassword()));
        userRepository.save(user);
    }

    @Transactional
    public void resetPassword(ResetPasswordRequest request) {
        Optional<EmailVerification> verificationOpt = emailVerificationRepository
                .findByEmailAndCodeAndVerifiedFalse(request.getEmail(), request.getCode());

        if (verificationOpt.isEmpty()) throw new IllegalArgumentException("인증 코드가 올바르지 않습니다");

        EmailVerification verification = verificationOpt.get();
        if (verification.isExpired()) throw new IllegalArgumentException("인증 코드가 만료되었습니다");

        User user = userRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new IllegalArgumentException("가입된 이메일이 아닙니다"));

        if (user.getProvider() != AuthProvider.EMAIL) {
            throw new IllegalArgumentException("소셜 로그인 계정은 비밀번호 재설정을 지원하지 않습니다");
        }

        user.updatePassword(passwordEncoder.encode(request.getNewPassword()));
        userRepository.save(user);

        verification.verify();
        emailVerificationRepository.save(verification);
    }

    @Transactional
    public void checkNicknameDuplicate(String nickname) {
        if (userRepository.existsByNickname(nickname)) {
            throw new IllegalArgumentException("이미 사용 중인 닉네임입니다");
        }
    }
}
