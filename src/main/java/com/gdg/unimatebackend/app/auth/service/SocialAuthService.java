package com.gdg.unimatebackend.app.auth.service;

import com.gdg.unimatebackend.app.auth.dto.AuthTokenResponse;
import com.gdg.unimatebackend.app.auth.dto.KakaoLoginRequest;
import com.gdg.unimatebackend.app.auth.dto.KakaoUserResponse;
import com.gdg.unimatebackend.app.user.entity.AuthProvider;
import com.gdg.unimatebackend.app.user.entity.User;
import com.gdg.unimatebackend.app.user.repository.UserRepository;
import com.gdg.unimatebackend.global.security.jwt.JwtUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class SocialAuthService {

    private final KakaoApiService kakaoApiService;
    private final UserRepository userRepository;
    private final JwtUtil jwtUtil;

    @Transactional
    public AuthTokenResponse kakaoLogin(KakaoLoginRequest request) {
        KakaoUserResponse kakaoUser = kakaoApiService.getUserInfo(request.accessToken());
        return upsertAndIssueJwt(kakaoUser);
    }

    @Transactional
    public AuthTokenResponse kakaoLoginByCode(String code) {
        String accessToken = kakaoApiService.getAccessToken(code);
        KakaoUserResponse kakaoUser = kakaoApiService.getUserInfo(accessToken);
        return upsertAndIssueJwt(kakaoUser);
    }

    public String buildKakaoAuthorizeUrl() {
        return kakaoApiService.buildAuthorizeUrl();
    }

    private AuthTokenResponse upsertAndIssueJwt(KakaoUserResponse me) {
        if (me == null || me.id() == null) {
            throw new IllegalArgumentException("Kakao user id is required");
        }

        String providerId = String.valueOf(me.id());

        // Travodo 톤: 이메일이 없을 때도 시스템이 굴러가게
        String email = (me.email() != null && !me.email().isBlank())
                ? me.email()
                : providerId + "@kakao.unimate";

        User user = userRepository.findByProviderAndProviderId(AuthProvider.KAKAO, providerId)
                .orElseGet(() -> User.createKakaoUser(providerId, email, me.nickname()));

        // Travodo 톤: 로그인 때마다 최신 외부 프로필로 동기화
        user.updateProfile(me.nickname());
        user.updateProfileImageUrl(me.profileImageUrl());

        User saved = userRepository.save(user);

        // ✅ 최소 구현 유지
        String accessToken = jwtUtil.generateToken(saved.getId());
        String refreshToken = jwtUtil.generateToken(saved.getId());

        return new AuthTokenResponse(
                accessToken,
                refreshToken,
                saved.getId(),
                saved.getProvider().name(),
                saved.getProviderId(),
                saved.getNickname(),
                saved.getProfileImageUrl()
        );
    }
}
