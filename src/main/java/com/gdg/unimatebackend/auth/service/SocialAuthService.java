package com.gdg.unimatebackend.auth.service;

import com.gdg.unimatebackend.auth.dto.AuthResponse;
import com.gdg.unimatebackend.user.entity.AuthProvider;
import com.gdg.unimatebackend.user.entity.User;
import com.gdg.unimatebackend.user.repository.UserRepository;
import com.gdg.unimatebackend.global.security.jwt.JwtUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@Service
@RequiredArgsConstructor
public class SocialAuthService {

    private final UserRepository userRepository;
    private final JwtUtil jwtUtil;
    private final RefreshTokenService refreshTokenService;

    private final KakaoApiService kakaoClient;
    private final NaverApiService naverClient;
    private final GoogleApiService googleClient;

    @Transactional
    public AuthResponse loginByAccessToken(AuthProvider provider, String accessToken) {
        if (provider == AuthProvider.KAKAO) {
            var me = kakaoClient.fetchUserInfo(accessToken);
            return upsertAndIssueJwt(AuthProvider.KAKAO,
                    String.valueOf(me.getId()),
                    me.email(),
                    me.nicknameOrDefault()
            );
        }

        if (provider == AuthProvider.NAVER) {
            var me = naverClient.fetchUserInfo(accessToken);
            if (me == null || me.response() == null || me.response().id() == null) {
                throw new IllegalArgumentException("네이버 사용자 정보 조회 실패");
            }
            return upsertAndIssueJwt(AuthProvider.NAVER,
                    me.response().id(),
                    me.response().email(),
                    (me.response().nickname() == null || me.response().nickname().isBlank()) ? "네이버사용자" : me.response().nickname()
            );
        }

        if (provider == AuthProvider.GOOGLE) {
            var me = googleClient.fetchUserInfoFromIdToken(accessToken);
            return upsertAndIssueJwt(
                    AuthProvider.GOOGLE,
                    me.id(),
                    me.email(),
                    me.name()
            );
        }

        throw new UnsupportedOperationException("지원하지 않는 provider: " + provider);
    }

    /**
     * providerId 기준으로 유저를 찾고 없으면 생성한다.
     * - 이메일이 없으면 대체 이메일을 만든다.
     * - 닉네임 중복이면 suffix를 붙인다.
     */
    private AuthResponse upsertAndIssueJwt(AuthProvider provider, String providerId, String email, String nickname) {
        Optional<User> existing = userRepository.findByProviderAndProviderId(provider, providerId);

        User user = existing.orElseGet(() -> createSocialUser(provider, providerId, email, nickname));

        if (!user.getActive()) {
            throw new IllegalArgumentException("탈퇴한 계정입니다");
        }

        String token = jwtUtil.generateToken(user.getId(), user.getEmail());
        String refreshToken = refreshTokenService.issueForUser(user);
        return AuthResponse.builder()
                .token(token)
                .refreshToken(refreshToken)
                .userId(user.getId())
                .email(user.getEmail())
                .nickname(user.getNickname())
                .build();
    }

    private User createSocialUser(AuthProvider provider, String providerId, String email, String nickname) {
        String finalEmail = (email == null || email.isBlank())
                ? generateAlternativeEmail(provider, providerId)
                : email;

        // email 중복 방지
        String emailCandidate = finalEmail;
        int emailSuffix = 1;
        while (userRepository.findByEmail(emailCandidate).isPresent()) {
            String[] parts = finalEmail.split("@", 2);
            emailCandidate = parts[0] + "_" + emailSuffix + "@" + parts[1];
            emailSuffix++;
        }

        // nickname 중복 방지
        String nickCandidate = (nickname == null || nickname.isBlank()) ? provider.name().toLowerCase() + "User" : nickname;
        int nickSuffix = 1;
        while (userRepository.existsByNickname(nickCandidate)) {
            nickCandidate = nickname + "_" + nickSuffix;
            nickSuffix++;
        }

        User user = User.builder()
                .email(emailCandidate)
                .password(null)
                .nickname(nickCandidate)
                .emailVerified(email != null && !email.isBlank())
                .provider(provider)
                .providerId(providerId)
                .active(true)
                .build();

        return userRepository.save(user);
    }

    private String generateAlternativeEmail(AuthProvider provider, String providerId) {
        return provider.name().toLowerCase() + "_" + providerId + "@unimate.local";
    }
}
