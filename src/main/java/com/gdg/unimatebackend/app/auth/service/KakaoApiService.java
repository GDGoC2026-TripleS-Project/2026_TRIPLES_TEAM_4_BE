package com.gdg.unimatebackend.app.auth.service;

import com.gdg.unimatebackend.app.auth.dto.KakaoUserResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import java.util.Map;

@Service
@RequiredArgsConstructor
public class KakaoApiService {

    private final RestTemplate restTemplate = new RestTemplate();

    // ✅ application.yml 건드리지 않기 위해 "기본값"을 포함한다.
    @Value("${oauth.kakao.client-id:}")
    private String clientId;

    @Value("${oauth.kakao.redirect-uri:}")
    private String redirectUri;

    @Value("${oauth.kakao.authorize-uri:https://kauth.kakao.com/oauth/authorize}")
    private String authorizeUri;

    @Value("${oauth.kakao.token-uri:https://kauth.kakao.com/oauth/token}")
    private String tokenUri;

    @Value("${oauth.kakao.user-info-uri:https://kapi.kakao.com/v2/user/me}")
    private String userInfoUri;

    /**
     * ✅ SocialAuthService에서 호출하는 메서드 (빨간줄 해결 1)
     */
    public String buildAuthorizeUrl() {
        // clientId/redirectUri가 비어있으면 URL이 깨지니, 즉시 확인되게 예외
        if (clientId == null || clientId.isBlank()) {
            throw new IllegalStateException("oauth.kakao.client-id is empty");
        }
        if (redirectUri == null || redirectUri.isBlank()) {
            throw new IllegalStateException("oauth.kakao.redirect-uri is empty");
        }

        return UriComponentsBuilder.fromUriString(authorizeUri)
                .queryParam("response_type", "code")
                .queryParam("client_id", clientId)
                .queryParam("redirect_uri", redirectUri)
                .build()
                .toUriString();
    }

    /**
     * ✅ SocialAuthService에서 호출하는 메서드 (빨간줄 해결 2)
     * 인가코드(code) → 카카오 access_token 교환
     */
    public String getAccessToken(String code) {
        if (code == null || code.isBlank()) {
            throw new IllegalArgumentException("code is required");
        }
        if (clientId == null || clientId.isBlank()) {
            throw new IllegalStateException("oauth.kakao.client-id is empty");
        }
        if (redirectUri == null || redirectUri.isBlank()) {
            throw new IllegalStateException("oauth.kakao.redirect-uri is empty");
        }

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);

        MultiValueMap<String, String> form = new LinkedMultiValueMap<>();
        form.add("grant_type", "authorization_code");
        form.add("client_id", clientId);
        form.add("redirect_uri", redirectUri);
        form.add("code", code);

        HttpEntity<MultiValueMap<String, String>> request = new HttpEntity<>(form, headers);

        ResponseEntity<Map> response = restTemplate.exchange(
                tokenUri,
                HttpMethod.POST,
                request,
                Map.class
        );

        Object token = (response.getBody() == null) ? null : response.getBody().get("access_token");
        if (token == null) {
            throw new IllegalStateException("Kakao token response has no access_token");
        }
        return String.valueOf(token);
    }

    /**
     * ✅ access_token으로 카카오 사용자 정보 조회
     */
    public KakaoUserResponse getUserInfo(String accessToken) {
        if (accessToken == null || accessToken.isBlank()) {
            throw new IllegalArgumentException("accessToken is required");
        }

        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(accessToken);
        headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);

        HttpEntity<Void> request = new HttpEntity<>(headers);

        ResponseEntity<KakaoUserResponse> response = restTemplate.exchange(
                userInfoUri,
                HttpMethod.GET,
                request,
                KakaoUserResponse.class
        );

        KakaoUserResponse body = response.getBody();
        if (body == null) {
            throw new IllegalStateException("Kakao user info response is empty");
        }
        return body;
    }
}
