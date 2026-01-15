package com.gdg.unimatebackend.app.auth.service;

import com.gdg.unimatebackend.app.auth.dto.AuthTokenResponse;
import com.gdg.unimatebackend.app.auth.dto.KakaoLoginRequest;
import com.gdg.unimatebackend.app.auth.dto.KakaoTokenResponse;
import com.gdg.unimatebackend.app.auth.dto.KakaoUserResponse;
import com.gdg.unimatebackend.app.auth.service.KakaoApiService;
import com.gdg.unimatebackend.app.auth.service.KakaoOAuthClient;
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

    private final KakaoOAuthClient kakaoOAuthClient;
    private final KakaoApiService kakaoApiService;
    private final UserRepository userRepository;
    private final JwtUtil jwtUtil;

    public String buildKakaoAuthorizeUrl() {
        return kakaoOAuthClient.buildAuthorizeUrl();
    }

    @Transactional
    public AuthTokenResponse kakaoLogin(KakaoLoginRequest request) {
        String accessToken = request.accessToken();
        if (accessToken == null || accessToken.isBlank()) {
            throw new IllegalArgumentException("accessToken is required.");
        }

        KakaoUserResponse me = kakaoApiService.getUserInfo(accessToken.trim());
        return upsertAndIssueJwt(me);
    }

    @Transactional
    public AuthTokenResponse kakaoLoginByCode(String code) {
        if (code == null || code.isBlank()) {
            throw new IllegalArgumentException("code is required.");
        }

        KakaoTokenResponse token = kakaoOAuthClient.exchangeCodeToToken(code.trim());
        KakaoUserResponse me = kakaoApiService.getUserInfo(token.accessToken());
        return upsertAndIssueJwt(me);
    }

    private AuthTokenResponse upsertAndIssueJwt(KakaoUserResponse me) {
        if (me == null || me.id() == null) {
            throw new IllegalArgumentException("Kakao user id is required.");
        }

        String providerId = String.valueOf(me.id());

        // ✅ 재할당 없는 값으로 고정 (람다 캡처 OK)
        final String finalEmail =
                (me.email() != null && !me.email().isBlank())
                        ? me.email()
                        : providerId + "@unimate.local";

        User user = userRepository.findByProviderAndProviderId(AuthProvider.KAKAO, providerId)
                .orElseGet(() -> User.createKakaoUser(providerId, finalEmail, me.nickname()));

        // 기존 유저도 닉네임 최신화(선택)
        user.updateProfile(me.nickname());

        User saved = userRepository.save(user);

        String accessToken = jwtUtil.generateToken(saved.getId());
        String refreshToken = jwtUtil.generateToken(saved.getId()); // 임시 동일

        return new AuthTokenResponse(
                accessToken,
                refreshToken,
                saved.getId(),
                String.valueOf(saved.getProvider()),
                saved.getProviderId(),
                saved.getNickname(),
                null
        );
    }
}
