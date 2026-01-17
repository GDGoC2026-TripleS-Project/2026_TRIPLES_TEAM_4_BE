package com.gdg.unimatebackend.app.auth.service;

import com.gdg.unimatebackend.app.auth.dto.AuthResponse;
import com.gdg.unimatebackend.app.auth.service.KakaoApiService;
import com.gdg.unimatebackend.app.user.entity.AuthProvider;
import com.gdg.unimatebackend.app.user.entity.User;
import com.gdg.unimatebackend.app.user.repository.UserRepository;
import com.gdg.unimatebackend.global.exception.AccountLinkRequiredException;
import com.gdg.unimatebackend.global.security.jwt.JwtUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@Service
@RequiredArgsConstructor
@Slf4j
public class SocialAuthService {

    private final UserRepository userRepository;
    private final JwtUtil jwtUtil;
    private final KakaoApiService kakaoApiService;

    @Transactional
    public AuthResponse socialLogin(AuthProvider provider, String accessToken, String email, String nickname) {
        if (provider == AuthProvider.KAKAO) {
            return kakaoLogin(accessToken);
        } else {
            throw new UnsupportedOperationException("지원하지 않는 소셜 로그인 제공자입니다: " + provider);
        }
    }

    /**
     * 카카오 로그인 처리
     *
     * @param accessToken 카카오 액세스 토큰
     * @return 인증 응답 (JWT 토큰 포함)
     */
    private AuthResponse kakaoLogin(String accessToken) {
        // 카카오 API로 사용자 정보 조회
        KakaoApiService.KakaoUserInfo kakaoUserInfo = kakaoApiService.getUserInfo(accessToken);

        String providerId = String.valueOf(kakaoUserInfo.getId());
        String email = kakaoUserInfo.getEmail();
        String nickname = kakaoUserInfo.getNickname();

        // providerId로 기존 사용자 찾기
        Optional<User> existingUserOpt = userRepository.findByProviderAndProviderId(AuthProvider.KAKAO, providerId);

        User user;
        if (existingUserOpt.isPresent()) {
            // 기존 사용자 로그인
            user = existingUserOpt.get();
            if (!user.getActive()) {
                throw new IllegalArgumentException("탈퇴한 계정입니다");
            }
            log.info("카카오 로그인 - 기존 사용자: {}", user.getEmail());
        } else {
            // 이메일로 이미 가입된 사용자가 있는지 확인 (계정 통합)
            if (email != null && !email.isEmpty() && !email.contains("@Unimate.local")) {
                Optional<User> emailUserOpt = userRepository.findByEmail(email);
                if (emailUserOpt.isPresent()) {
                    User emailUser = emailUserOpt.get();

                    // 이미 카카오로 가입된 경우는 위에서 처리됨 (providerId로 찾기)
                    // 이메일로 가입된 계정인 경우: 계정 통합 확인 필요
                    if (emailUser.getProvider() == AuthProvider.EMAIL && emailUser.getPassword() != null) {
                        // 사용자에게 계정 통합 확인 요청
                        throw new AccountLinkRequiredException(AuthProvider.EMAIL, email);
                    } else {
                        // 다른 소셜 로그인으로 가입된 경우
                        throw new IllegalArgumentException(
                                "이미 " + emailUser.getProvider() + " 로그인으로 가입된 이메일입니다"
                        );
                    }
                } else {
                    // 새 사용자 생성
                    user = createSocialUser(AuthProvider.KAKAO, providerId, email, nickname);
                    log.info("카카오 로그인 - 신규 사용자 생성: {}", user.getEmail());
                }
            } else {
                // 이메일이 없는 경우 새 사용자 생성
                user = createSocialUser(AuthProvider.KAKAO, providerId, email, nickname);
                log.info("카카오 로그인 - 신규 사용자 생성: {}", user.getEmail());
            }
        }

        // JWT 토큰 생성
        String token = jwtUtil.generateToken(user.getId(), user.getEmail());

        return AuthResponse.builder()
                .token(token)
                .userId(user.getId())
                .email(user.getEmail())
                .nickname(user.getNickname())
                .build();
    }

    /**
     * 소셜 로그인 사용자 생성
     *
     * @param provider 로그인 제공자
     * @param providerId 소셜 로그인 제공자의 사용자 ID
     * @param email 이메일 (null일 수 있음)
     * @param nickname 닉네임
     * @return 생성된 User 엔티티
     */
    private User createSocialUser(AuthProvider provider, String providerId, String email, String nickname) {
        // 이메일이 없는 경우 대체 이메일 생성 (카카오에서 이메일 제공을 거부한 경우)
        String finalEmail = email;
        if (finalEmail == null || finalEmail.isEmpty()) {
            finalEmail = generateAlternativeEmail(provider, providerId);
            log.info("카카오 로그인 - 이메일 미제공, 대체 이메일 생성: {}", finalEmail);
        }

        // 이메일 중복 확인 및 처리 (대체 이메일이 이미 존재하는 경우)
        int emailSuffix = 1;
        String originalEmail = finalEmail;
        while (userRepository.findByEmail(finalEmail).isPresent()) {
            String[] emailParts = originalEmail.split("@");
            finalEmail = emailParts[0] + "_" + emailSuffix + "@" + emailParts[1];
            emailSuffix++;
        }

        // 닉네임 중복 확인 및 처리
        String finalNickname = nickname;
        int suffix = 1;
        while (userRepository.existsByNickname(finalNickname)) {
            finalNickname = nickname + "_" + suffix;
            suffix++;
        }

        User user = User.builder()
                .email(finalEmail)
                .password(null)  // 소셜 로그인 사용자는 비밀번호 없음
                .nickname(finalNickname)
                .emailVerified(email != null && !email.isEmpty())  // 실제 이메일이 있으면 인증 완료, 없으면 false
                .provider(provider)
                .providerId(providerId)
                .active(true)
                .build();

        return userRepository.save(user);
    }

    /**
     * 소셜 로그인에서 이메일이 제공되지 않은 경우 대체 이메일 생성
     *
     * @param provider 로그인 제공자
     * @param providerId 소셜 로그인 제공자의 사용자 ID
     * @return 대체 이메일 주소
     */
    private String generateAlternativeEmail(AuthProvider provider, String providerId) {
        return provider.name().toLowerCase() + "_" + providerId + "@Unimate.local";
    }

    /**
     * 계정 통합: 이메일 계정에 소셜 로그인 providerId 추가
     *
     * @param email 이메일
     * @param provider 소셜 로그인 제공자
     * @param providerId 소셜 로그인 제공자의 사용자 ID
     * @return 인증 응답
     */
    @Transactional
    public AuthResponse linkSocialAccount(String email, AuthProvider provider, String providerId) {
        // 기존 이메일 계정 찾기
        User existingUser = userRepository.findByEmail(email)
                .orElseThrow(() -> new IllegalArgumentException("계정을 찾을 수 없습니다"));

        // 이메일 계정인지 확인
        if (existingUser.getProvider() != AuthProvider.EMAIL || existingUser.getPassword() == null) {
            throw new IllegalArgumentException("이메일 로그인 계정이 아닙니다");
        }

        // 이미 해당 providerId로 연동된 경우
        if (existingUser.getProviderId() != null && existingUser.getProviderId().equals(providerId)) {
            log.info("이미 연동된 계정: {}", email);
        } else {
            // 카카오 providerId 추가
            existingUser.linkSocialProvider(providerId);
            existingUser = userRepository.save(existingUser);
            log.info("카카오 로그인 - 기존 이메일 계정에 카카오 연동: {}", email);
        }

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

