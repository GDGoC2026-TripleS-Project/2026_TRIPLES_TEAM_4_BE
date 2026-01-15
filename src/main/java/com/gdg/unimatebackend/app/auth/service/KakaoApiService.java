package com.gdg.unimatebackend.app.auth.service;

import com.gdg.unimatebackend.app.auth.dto.KakaoUserResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import java.util.Map;

@Service
@RequiredArgsConstructor
public class KakaoApiService {

    private final RestTemplate restTemplate = new RestTemplate();

    @Value("${oauth.kakao.client-id:}")
    private String clientId;

    @Value("${oauth.kakao.client-secret:}")
    private String clientSecret;

    @Value("${oauth.kakao.redirect-uri:}")
    private String redirectUri;

    @Value("${oauth.kakao.authorize-uri:https://kauth.kakao.com/oauth/authorize}")
    private String authorizeUri;

    @Value("${oauth.kakao.token-uri:https://kauth.kakao.com/oauth/token}")
    private String tokenUri;

    @Value("${oauth.kakao.user-info-uri:https://kapi.kakao.com/v2/user/me}")
    private String userInfoUri;

    /**
     * 카카오 로그인 화면으로 이동할 Authorize URL 생성
     */
    public String buildAuthorizeUrl() {
        requireText(clientId, "oauth.kakao.client-id is empty");
        requireText(redirectUri, "oauth.kakao.redirect-uri is empty");

        return UriComponentsBuilder.fromUriString(authorizeUri)
                .queryParam("response_type", "code")
                .queryParam("client_id", clientId)
                .queryParam("redirect_uri", redirectUri)
                .build()
                .toUriString();
    }

    /**
     * 인가코드(code) → 카카오 access_token 교환
     */
    public String getAccessToken(String code) {
        requireText(code, "code is required");
        requireText(clientId, "oauth.kakao.client-id is empty");
        requireText(redirectUri, "oauth.kakao.redirect-uri is empty");

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);

        MultiValueMap<String, String> form = new LinkedMultiValueMap<>();
        form.add("grant_type", "authorization_code");
        form.add("client_id", clientId);
        form.add("redirect_uri", redirectUri);
        form.add("code", code);

        // ✅ 핵심: Client Secret 사용 ON이면 필요할 수 있음 (401 방지)
        if (clientSecret != null && !clientSecret.isBlank()) {
            form.add("client_secret", clientSecret);
        }

        HttpEntity<MultiValueMap<String, String>> request = new HttpEntity<>(form, headers);

        try {
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

        } catch (HttpClientErrorException e) {
            // 카카오가 401/400을 body 없이 줄 때가 있어서 status라도 보이게
            throw new IllegalStateException(
                    "Kakao token request failed. status=" + e.getStatusCode(),
                    e
            );
        }
    }

    /**
     * access_token으로 카카오 사용자 정보 조회
     */
    public KakaoUserResponse getUserInfo(String accessToken) {
        requireText(accessToken, "accessToken is required");

        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(accessToken);

        HttpEntity<Void> request = new HttpEntity<>(headers);

        try {
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

        } catch (HttpClientErrorException e) {
            throw new IllegalStateException(
                    "Kakao user-info request failed. status=" + e.getStatusCode(),
                    e
            );
        }
    }

    private void requireText(String value, String message) {
        if (value == null || value.isBlank()) {
            throw new IllegalStateException(message);
        }
    }
}
