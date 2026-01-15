package com.gdg.unimatebackend.app.auth.service;

import com.gdg.unimatebackend.app.auth.dto.KakaoTokenResponse;
import com.gdg.unimatebackend.global.exception.KakaoApiException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

@Service
@Slf4j
@RequiredArgsConstructor
public class KakaoOAuthClient {

    // ✅ RestTemplate을 new로 만들지 말고, Spring Bean 주입받는 방식이 가장 안전
    private final RestTemplate restTemplate;

    @Value("${oauth.kakao.client-id}")
    private String clientId;

    /**
     * ✅ 네 앱 상태상 client_secret이 사실상 필수.
     * 값이 비어있으면 바로 실패하게 해서, 401을 환경설정 문제로 빠르게 분리한다.
     */
    @Value("${oauth.kakao.client-secret}")
    private String clientSecret;

    @Value("${oauth.kakao.redirect-uri}")
    private String redirectUri;

    @Value("${oauth.kakao.authorize-uri:https://kauth.kakao.com/oauth/authorize}")
    private String authorizeUri;

    @Value("${oauth.kakao.token-uri:https://kauth.kakao.com/oauth/token}")
    private String tokenUri;

    public String buildAuthorizeUrl() {
        return UriComponentsBuilder.fromUriString(authorizeUri)
                .queryParam("response_type", "code")
                .queryParam("client_id", clientId)
                .queryParam("redirect_uri", redirectUri)
                .build(true) // ✅ 인코딩 관련 이슈 줄이기
                .toUriString();
    }

    public KakaoTokenResponse exchangeCodeToToken(String code) {
        validateConfig();
        if (code == null || code.isBlank()) {
            throw new KakaoApiException("인가 코드(code)가 비어있습니다.");
        }

        try {
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);

            MultiValueMap<String, String> body = new LinkedMultiValueMap<>();
            body.add("grant_type", "authorization_code");
            body.add("client_id", clientId);
            body.add("redirect_uri", redirectUri);
            body.add("code", code);
            body.add("client_secret", clientSecret); // ✅ 필수로 항상 포함

            HttpEntity<MultiValueMap<String, String>> entity = new HttpEntity<>(body, headers);

            // ✅ 민감정보 마스킹 로그 (client_secret 전체 노출 금지)
            log.info("[KakaoToken] endpoint={}", tokenUri);
            log.info("[KakaoToken] client_id={}, redirect_uri={}", mask(clientId, 6), redirectUri);
            log.info("[KakaoToken] code={}", mask(code, 8));
            log.info("[KakaoToken] client_secret_present={}", !clientSecret.isBlank());

            ResponseEntity<KakaoTokenResponse> response = restTemplate.exchange(
                    tokenUri,
                    HttpMethod.POST,
                    entity,
                    KakaoTokenResponse.class
            );

            KakaoTokenResponse token = response.getBody();
            if (response.getStatusCode().is2xxSuccessful() && token != null && token.accessToken() != null) {
                return token;
            }

            throw new KakaoApiException("카카오 토큰 발급 실패: empty body or missing access_token");
        } catch (HttpClientErrorException e) {
            String respBody = e.getResponseBodyAsString();
            log.error("[KakaoToken] fail status={}, body={}", e.getStatusCode(), (respBody == null || respBody.isBlank()) ? "<empty>" : respBody);
            throw new KakaoApiException("카카오 토큰 발급 실패: " + e.getStatusCode(), e, e.getStatusCode().value());
        } catch (Exception e) {
            throw new KakaoApiException("카카오 토큰 발급 중 오류", e);
        }
    }

    private void validateConfig() {
        if (clientId == null || clientId.isBlank()) {
            throw new KakaoApiException("oauth.kakao.client-id 설정이 비어있습니다.");
        }
        if (redirectUri == null || redirectUri.isBlank()) {
            throw new KakaoApiException("oauth.kakao.redirect-uri 설정이 비어있습니다.");
        }
        if (clientSecret == null || clientSecret.isBlank()) {
            throw new KakaoApiException("oauth.kakao.client-secret 설정이 비어있습니다. (현재 앱은 client_secret 필수)");
        }
    }

    private String mask(String value, int keepPrefix) {
        if (value == null) return "null";
        if (value.length() <= keepPrefix) return value;
        return value.substring(0, keepPrefix) + "****";
    }
}
