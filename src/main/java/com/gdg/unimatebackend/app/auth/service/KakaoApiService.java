package com.gdg.unimatebackend.app.auth.service;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.gdg.unimatebackend.global.exception.KakaoApiException;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;

/**
 * 카카오 API 호출 서비스
 * 카카오 액세스 토큰을 검증하고 사용자 정보를 가져옵니다.
 */
@Service
@Slf4j
public class KakaoApiService implements InitializingBean {

    private final RestTemplate restTemplate;
    private final String kakaoApiBaseUrl = "https://kapi.kakao.com";

    // 카카오 OAuth 토큰 교환 엔드포인트
    @Value("${oauth.kakao.token-uri:https://kauth.kakao.com/oauth/token}")
    private String tokenUri;

    // 카카오 REST API Key (client_id)
    @Value("${oauth.kakao.client-id:}")
    private String restApiKey;

    // 카카오 앱 설정에 따라 필요할 수도/없을 수도 있어 optional 처리
    @Value("${oauth.kakao.client-secret:}")
    private String clientSecret;

    public KakaoApiService() {
        this.restTemplate = new RestTemplate();
    }

    @Override
    public void afterPropertiesSet() {
        if (restApiKey == null || restApiKey.isEmpty()) {
            log.warn("카카오 REST API 키가 설정되지 않았습니다. 카카오 로그인이 작동하지 않을 수 있습니다.");
        } else {
            log.info("카카오 REST API 키가 설정되었습니다. (키: {}...)", restApiKey.substring(0, Math.min(8, restApiKey.length())));
        }
    }

    // =========================
    // code -> access_token 교환
    // =========================

    @Getter
    @Setter
    public static class KakaoTokenResponse {
        @JsonProperty("access_token")
        private String accessToken;

        @JsonProperty("token_type")
        private String tokenType;

        @JsonProperty("refresh_token")
        private String refreshToken;

        @JsonProperty("expires_in")
        private Integer expiresIn;

        @JsonProperty("scope")
        private String scope;

        @JsonProperty("error")
        private String error;

        @JsonProperty("error_description")
        private String errorDescription;
    }

    /**
     * 인가 코드(code)를 카카오 access_token으로 교환
     */
    public KakaoTokenResponse exchangeCodeToToken(String code, String redirectUri) {
        try {
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);

            MultiValueMap<String, String> body = new LinkedMultiValueMap<>();
            body.add("grant_type", "authorization_code");
            body.add("client_id", restApiKey);
            body.add("redirect_uri", redirectUri);
            body.add("code", code);

            // client_secret을 쓰는 앱이면 추가
            if (clientSecret != null && !clientSecret.isBlank()) {
                body.add("client_secret", clientSecret);
            }

            HttpEntity<MultiValueMap<String, String>> entity = new HttpEntity<>(body, headers);

            ResponseEntity<KakaoTokenResponse> response = restTemplate.exchange(
                    tokenUri,
                    HttpMethod.POST,
                    entity,
                    KakaoTokenResponse.class
            );

            KakaoTokenResponse res = response.getBody();
            if (response.getStatusCode().is2xxSuccessful() && res != null && res.getAccessToken() != null) {
                return res;
            }

            throw new KakaoApiException("카카오 토큰 교환 실패", null,
                    response.getStatusCode().value());
        } catch (HttpClientErrorException e) {
            log.error("카카오 토큰 교환 HTTP 오류 ({}): {}", e.getStatusCode(), e.getMessage());
            log.error("응답 본문: {}", e.getResponseBodyAsString());
            throw new KakaoApiException(
                    "카카오 토큰 교환 중 오류가 발생했습니다: " + e.getStatusCode(),
                    e,
                    e.getStatusCode().value()
            );
        } catch (Exception e) {
            log.error("카카오 토큰 교환 중 오류 발생: {}", e.getMessage(), e);
            throw new KakaoApiException("카카오 토큰 교환 중 오류가 발생했습니다", e);
        }
    }

    /**
     * 카카오 액세스 토큰으로 사용자 정보 조회
     *
     * @param accessToken 카카오 액세스 토큰
     * @return 카카오 사용자 정보
     * @throws RuntimeException 토큰이 유효하지 않거나 API 호출 실패 시
     */
    public KakaoUserInfo getUserInfo(String accessToken) {
        try {
            HttpHeaders headers = new HttpHeaders();
            headers.set("Authorization", "Bearer " + accessToken);
            headers.set("Content-Type", "application/x-www-form-urlencoded;charset=utf-8");

            HttpEntity<String> entity = new HttpEntity<>(headers);

            String url = kakaoApiBaseUrl + "/v2/user/me?property_keys=[\"kakao_account.email\"]";
            ResponseEntity<KakaoUserInfo> response = restTemplate.exchange(
                    url,
                    HttpMethod.GET,
                    entity,
                    KakaoUserInfo.class
            );

            if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                return response.getBody();
            }

            throw new RuntimeException("카카오 사용자 정보 조회 실패");
        } catch (HttpClientErrorException.Unauthorized e) {
            log.error("카카오 액세스 토큰이 유효하지 않습니다: {}", e.getMessage());
            throw new KakaoApiException("유효하지 않은 카카오 액세스 토큰입니다", e, 401);
        } catch (HttpClientErrorException.Forbidden e) {
            log.error("카카오 API 호출 권한 오류 (403): {}", e.getMessage());
            log.error("응답 본문: {}", e.getResponseBodyAsString());
            throw new KakaoApiException(
                    "카카오 로그인 권한이 없습니다. 카카오 개발자 콘솔에서 플랫폼 등록 및 카카오 로그인 제품 활성화를 확인해주세요.",
                    e,
                    403
            );
        } catch (HttpClientErrorException e) {
            log.error("카카오 API HTTP 오류 ({}): {}", e.getStatusCode(), e.getMessage());
            log.error("응답 본문: {}", e.getResponseBodyAsString());
            throw new KakaoApiException(
                    "카카오 API 호출 중 오류가 발생했습니다: " + e.getStatusCode(),
                    e,
                    e.getStatusCode().value()
            );
        } catch (Exception e) {
            log.error("카카오 API 호출 중 오류 발생: {}", e.getMessage(), e);
            throw new KakaoApiException("카카오 사용자 정보 조회 중 오류가 발생했습니다", e);
        }
    }

    /**
     * 카카오 API 응답 DTO
     */
    @Getter
    @Setter
    public static class KakaoUserInfo {
        private Long id;

        @JsonProperty("kakao_account")
        private KakaoAccount kakaoAccount;

        @JsonProperty("properties")
        private KakaoProperties properties;

        @Getter
        @Setter
        public static class KakaoAccount {
            private String email;
            private Boolean emailNeedsAgreement;
            private Boolean isEmailValid;
            private Boolean isEmailVerified;

            @JsonProperty("profile")
            private KakaoProfile profile;

            @Getter
            @Setter
            public static class KakaoProfile {
                private String nickname;
                @JsonProperty("profile_image_url")
                private String profileImageUrl;
            }
        }

        @Getter
        @Setter
        public static class KakaoProperties {
            private String nickname;
            @JsonProperty("profile_image")
            private String profileImage;
        }

        public String getEmail() {
            if (kakaoAccount != null && kakaoAccount.getEmail() != null) {
                return kakaoAccount.getEmail();
            }
            return null;
        }

        public String getNickname() {
            if (properties != null && properties.getNickname() != null) {
                return properties.getNickname();
            }
            if (kakaoAccount != null && kakaoAccount.getProfile() != null
                    && kakaoAccount.getProfile().getNickname() != null) {
                return kakaoAccount.getProfile().getNickname();
            }
            return "카카오사용자";
        }
    }
}
