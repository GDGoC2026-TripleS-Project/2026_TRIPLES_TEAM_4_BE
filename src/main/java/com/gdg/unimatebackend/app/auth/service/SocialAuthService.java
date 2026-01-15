package com.gdg.unimatebackend.app.auth.service;

import com.gdg.unimatebackend.app.auth.dto.AuthTokenResponse;
import com.gdg.unimatebackend.app.auth.dto.KakaoLoginRequest;
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
    private final UserRepository userRepository;
    private final JwtUtil jwtUtil;

    @Transactional
    public AuthTokenResponse kakaoLogin(KakaoLoginRequest request) {
        String accessToken = resolveAccessToken(request);

        KakaoOAuthClient.KakaoUserInfo info = kakaoOAuthClient.fetchUserInfo(accessToken);

        // A안 전제: email은 필수 (미동의면 가입 불가 정책으로 처리)
        if (info.getEmail() == null || info.getEmail().isBlank()) {
            throw new IllegalArgumentException("Kakao email consent is required.");
        }

        // 1) 이메일로 계정 찾기 (Travodo 전제)
        User user = userRepository.findByEmail(info.getEmail())
                .orElseGet(() -> userRepository.save(
                        User.builder()
                                .email(info.getEmail())
                                .nickname(info.getNickname())
                                .provider(AuthProvider.KAKAO)
                                .providerId(info.getProviderId())
                                .build()
                ));

        // 2) 기존 계정이면 소셜 연결(또는 갱신)
        //    이미 다른 provider로 연결되어 있더라도 정책상 여기서 덮어쓸지/거부할지 선택 가능
        user.linkSocial(AuthProvider.KAKAO, info.getProviderId());

        // 3) 닉네임 업데이트(널이면 유지)
        user.updateProfile(info.getNickname());

        // 4) JWT 발급
        String token = jwtUtil.generateToken(user.getId());

        return AuthTokenResponse.builder()
                .token(token)
                .userId(user.getId())
                .provider(user.getProvider())
                .email(user.getEmail())
                .nickname(user.getNickname())
                .build();
    }

    private String resolveAccessToken(KakaoLoginRequest request) {
        if (request.getAccessToken() != null && !request.getAccessToken().isBlank()) {
            return request.getAccessToken();
        }
        if (request.getCode() != null && !request.getCode().isBlank()) {
            return kakaoOAuthClient.exchangeCodeToAccessToken(request.getCode(), request.getRedirectUri());
        }
        throw new IllegalArgumentException("Either accessToken or code must be provided.");
    }
}
