package com.gdg.unimatebackend.app.auth.service;

import com.gdg.unimatebackend.app.auth.dto.*;
import com.gdg.unimatebackend.app.user.entity.AuthProvider;
import com.gdg.unimatebackend.app.user.entity.EmailVerification;
import com.gdg.unimatebackend.app.user.entity.User;
import com.gdg.unimatebackend.app.user.repository.EmailVerificationRepository;
import com.gdg.unimatebackend.app.user.repository.UserRepository;
import com.gdg.unimatebackend.global.exception.AccountLinkRequiredException;
import com.gdg.unimatebackend.global.security.jwt.JwtUtil;
import lombok.RequiredArgsConstructor;
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

    private static final int VERIFICATION_CODE_EXPIRY_MINUTES = 10;

    @Transactional
    public void sendVerificationCode(String email) {
        // 이미 가입된 이메일인지 확인
        if (userRepository.existsByEmail(email)) {
            throw new IllegalArgumentException("이미 가입된 이메일입니다");
        }

        // 인증 코드 생성
        String code = emailService.generateVerificationCode();

        // 같은 이메일로 재요청 -> 기존 인증 정보 삭제
        emailVerificationRepository.findTopByEmailOrderByCreatedAtDesc(email)
                .ifPresent(verification -> {
                    if (!verification.getVerified() && !verification.isExpired()) {
                        emailVerificationRepository.delete(verification);
                    }
                });

        // 새 인증 정보 저장
        EmailVerification verification = EmailVerification.builder()
                .email(email)
                .code(code)
                .verified(false)
                .expiresAt(LocalDateTime.now().plusMinutes(VERIFICATION_CODE_EXPIRY_MINUTES))
                .build();

        emailVerificationRepository.save(verification);

        // 이메일 전송
        emailService.sendVerificationEmail(email, code);
    }

    @Transactional
    public void verifyEmailCode(String email, String code) {
        Optional<EmailVerification> verificationOpt = emailVerificationRepository
                .findByEmailAndCodeAndVerifiedFalse(email, code);

        if (verificationOpt.isEmpty()) {
            throw new IllegalArgumentException("인증 코드가 올바르지 않습니다");
        }

        EmailVerification verification = verificationOpt.get();

        if (verification.isExpired()) {
            throw new IllegalArgumentException("인증 코드가 만료되었습니다");
        }

        verification.verify();
        emailVerificationRepository.save(verification);
    }

    @Transactional
    public AuthResponse signup(SignupRequest request) {
        // 이메일 중복 확인
        Optional<User> existingUserOpt = userRepository.findByEmail(request.getEmail());

        if (existingUserOpt.isPresent()) {
            User existingUser = existingUserOpt.get();

            // 이미 이메일 로그인으로 가입된 경우
            if (existingUser.getProvider() == AuthProvider.EMAIL) {
                throw new IllegalArgumentException("이미 가입된 이메일입니다");
            }

            // 소셜 로그인으로 가입된 경우: 계정 통합 확인 필요
            if (existingUser.getPassword() == null) {
                // 사용자에게 계정 통합 확인 요청
                throw new AccountLinkRequiredException(existingUser.getProvider(), request.getEmail());
            }

            // 이미 비밀번호가 설정된 경우
            throw new IllegalArgumentException("이미 이메일 로그인 비밀번호가 설정된 계정입니다");
        }

        User user;

        // 새 사용자 생성 (이메일 로그인)
        // 닉네임 중복 확인
        if (userRepository.existsByNickname(request.getNickname())) {
            throw new IllegalArgumentException("이미 사용 중인 닉네임입니다");
        }

        // 이메일 인증 확인
        Optional<EmailVerification> verificationOpt = emailVerificationRepository
                .findTopByEmailOrderByCreatedAtDesc(request.getEmail());

        if (verificationOpt.isEmpty() || !verificationOpt.get().getVerified()) {
            throw new IllegalArgumentException("이메일 인증을 완료해주세요");
        }

        user = User.builder()
                .email(request.getEmail())
                .password(passwordEncoder.encode(request.getPassword()))
                .nickname(request.getNickname())
                .emailVerified(true)
                .provider(AuthProvider.EMAIL)
                .providerId(null)  // 이메일 로그인은 providerId 없음
                .active(true)
                .build();

        user = userRepository.save(user);

        // JWT 토큰 생성
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

        if (!user.getActive()) {
            throw new IllegalArgumentException("탈퇴한 계정입니다");
        }

        // 소셜 로그인 사용자는 비밀번호가 없으므로 이메일 로그인만 허용
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

    @Transactional
    public void deleteAccount(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("사용자를 찾을 수 없습니다"));

        user.deactivate();
        userRepository.save(user);
    }

    @Transactional
    public String findEmail(FindEmailRequest request) {
        // 이메일 인증 확인
        Optional<EmailVerification> verificationOpt = emailVerificationRepository
                .findByEmailAndCodeAndVerifiedFalse(request.getEmail(), request.getCode());

        if (verificationOpt.isEmpty()) {
            throw new IllegalArgumentException("인증 코드가 올바르지 않습니다");
        }

        EmailVerification verification = verificationOpt.get();

        if (verification.isExpired()) {
            throw new IllegalArgumentException("인증 코드가 만료되었습니다");
        }

        // 사용자 확인
        User user = userRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new IllegalArgumentException("가입된 이메일이 아닙니다"));

        return user.getEmail();
    }

    @Transactional
    public void changePassword(Long userId, ChangePasswordRequest request) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("사용자를 찾을 수 없습니다"));

        if (!passwordEncoder.matches(request.getCurrentPassword(), user.getPassword())) {
            throw new IllegalArgumentException("현재 비밀번호가 올바르지 않습니다");
        }

        user.updatePassword(passwordEncoder.encode(request.getNewPassword()));
        userRepository.save(user);
    }

    @Transactional
    public void resetPassword(ResetPasswordRequest request) {
        // 이메일 인증 확인
        Optional<EmailVerification> verificationOpt = emailVerificationRepository
                .findByEmailAndCodeAndVerifiedFalse(request.getEmail(), request.getCode());

        if (verificationOpt.isEmpty()) {
            throw new IllegalArgumentException("인증 코드가 올바르지 않습니다");
        }

        EmailVerification verification = verificationOpt.get();

        if (verification.isExpired()) {
            throw new IllegalArgumentException("인증 코드가 만료되었습니다");
        }

        User user = userRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new IllegalArgumentException("가입된 이메일이 아닙니다"));

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

    /**
     * 계정 통합: 소셜 로그인 계정에 이메일 로그인 비밀번호 추가
     *
     * @param request 계정 통합 요청
     * @return 인증 응답
     */
    @Transactional
    public AuthResponse linkAccount(LinkAccountRequest request) {
        // 기존 사용자 찾기
        User existingUser = userRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new IllegalArgumentException("계정을 찾을 수 없습니다"));

        // 제공자 확인
        if (existingUser.getProvider() != request.getExistingProvider()) {
            throw new IllegalArgumentException("계정 정보가 일치하지 않습니다");
        }

        // 이미 비밀번호가 설정된 경우
        if (existingUser.getPassword() != null && request.getExistingProvider() != AuthProvider.EMAIL) {
            throw new IllegalArgumentException("이미 비밀번호가 설정된 계정입니다");
        }

        // 이메일 인증 확인 (이메일 회원가입 시)
        if (request.getExistingProvider() != AuthProvider.EMAIL) {
            Optional<EmailVerification> verificationOpt = emailVerificationRepository
                    .findTopByEmailOrderByCreatedAtDesc(request.getEmail());

            if (verificationOpt.isEmpty() || !verificationOpt.get().getVerified()) {
                throw new IllegalArgumentException("이메일 인증을 완료해주세요");
            }
        }

        // 계정 통합
        if (request.getExistingProvider() == AuthProvider.KAKAO) {
            // 소셜 로그인 계정에 비밀번호 추가
            existingUser.updatePassword(passwordEncoder.encode(request.getPassword()));
            existingUser.verifyEmail(); // 이메일 인증 완료 처리

            // 닉네임 업데이트 (요청된 경우)
            if (request.getNickname() != null && !request.getNickname().isEmpty()) {
                // 닉네임 중복 확인
                if (!existingUser.getNickname().equals(request.getNickname())
                        && userRepository.existsByNickname(request.getNickname())) {
                    throw new IllegalArgumentException("이미 사용 중인 닉네임입니다");
                }
                // 닉네임은 User 엔티티에 setter가 없으므로 별도 처리 필요
            }
        } else if (request.getExistingProvider() == AuthProvider.EMAIL) {
            // 이메일 계정에 소셜 로그인 연동은 소셜 로그인 시 처리
            throw new IllegalArgumentException("이메일 계정에 소셜 로그인 연동은 소셜 로그인 시 처리됩니다");
        }

        existingUser = userRepository.save(existingUser);

        // JWT 토큰 생성
        String token = jwtUtil.generateToken(existingUser.getId(), existingUser.getEmail());

        return AuthResponse.builder()
                .token(token)
                .userId(existingUser.getId())
                .email(existingUser.getEmail())
                .nickname(existingUser.getNickname())
                .build();
    }
}

