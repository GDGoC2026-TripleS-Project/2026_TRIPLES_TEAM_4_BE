package com.gdg.unimatebackend.app.auth.service;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.gdg.unimatebackend.global.exception.KakaoApiException;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.InitializingBean;
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
public class KakaoApiService implements InitializingBean {

    private final RestTemplate restTemplate;

    @Value("${oauth.kakao.client-id:}")
    private String clientId;

    @Value("${oauth.kakao.client-secret:}")
    private String clientSecret;

    @Value("${oauth.kakao.authorize-uri:https://kauth.kakao.com/oauth/authorize}")
    private String authorizeUri;

    @Value("${oauth.kakao.token-uri:https://kauth.kakao.com/oauth/token}")
    private String tokenUri;

    @Value("${oauth.kakao.user-info-uri:https://kapi.kakao.com/v2/user/me}")
    private String userInfoUri;

    @Value("${oauth.kakao.redirect-uri}")
    private String redirectUri;

    @Override
    public void afterPropertiesSet() {
        if (clientId == null || clientId.isBlank()) {
            log.warn("Kakao client-id is empty. Kakao login may not work.");
        } else {
            log.info("Kakao client-id loaded. ({}...)", clientId.substring(0, Math.min(8, clientId.length())));
        }
    }

    public String buildAuthorizeUrl(String state) {
        return UriComponentsBuilder.fromUriString(authorizeUri)
                .queryParam("response_type", "code")
                .queryParam("client_id", clientId)
                .queryParam("redirect_uri", redirectUri)
                .queryParam("state", state)
                .toUriString();
    }

    public KakaoTokenResponse exchangeCodeForToken(String code) {
        try {
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);

            MultiValueMap<String, String> form = new LinkedMultiValueMap<>();
            form.add("grant_type", "authorization_code");
            form.add("client_id", clientId);
            form.add("redirect_uri", redirectUri);
            form.add("code", code);

            if (clientSecret != null && !clientSecret.isBlank()) {
                form.add("client_secret", clientSecret);
            }

            ResponseEntity<KakaoTokenResponse> res = restTemplate.exchange(
                    tokenUri,
                    HttpMethod.POST,
                    new HttpEntity<>(form, headers),
                    KakaoTokenResponse.class
            );

            KakaoTokenResponse body = res.getBody();
            if (res.getStatusCode().is2xxSuccessful() && body != null && body.accessToken != null && !body.accessToken.isBlank()) {
                return body;
            }

            throw new KakaoApiException("Kakao token exchange failed", null, res.getStatusCode().value());
        } catch (HttpClientErrorException e) {
            throw new KakaoApiException("Kakao token exchange http error: " + e.getStatusCode(), e, e.getStatusCode().value());
        }
    }

    public KakaoUserInfo fetchUserInfo(String accessToken) {
        try {
            HttpHeaders headers = new HttpHeaders();
            headers.setBearerAuth(accessToken);
            headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);

            ResponseEntity<KakaoUserInfo> res = restTemplate.exchange(
                    userInfoUri,
                    HttpMethod.GET,
                    new HttpEntity<>(headers),
                    KakaoUserInfo.class
            );

            KakaoUserInfo body = res.getBody();
            if (res.getStatusCode().is2xxSuccessful() && body != null && body.id != null) {
                return body;
            }
            throw new KakaoApiException("Kakao userinfo fetch failed", null, res.getStatusCode().value());
        } catch (HttpClientErrorException e) {
            throw new KakaoApiException("Kakao userinfo http error: " + e.getStatusCode(), e, e.getStatusCode().value());
        }
    }

    public String getRedirectUri() {
        return redirectUri;
    }

    // ===== DTOs =====

    @Getter
    public static class KakaoTokenResponse {
        @JsonProperty("access_token")
        private String accessToken;
        @JsonProperty("refresh_token")
        private String refreshToken;
        @JsonProperty("token_type")
        private String tokenType;
        @JsonProperty("expires_in")
        private Integer expiresIn;
        @JsonProperty("scope")
        private String scope;

        @JsonProperty("error")
        private String error;
        @JsonProperty("error_description")
        private String errorDescription;
    }

    @Getter
    public static class KakaoUserInfo {
        private Long id;

        @JsonProperty("kakao_account")
        private KakaoAccount kakaoAccount;

        @JsonProperty("properties")
        private KakaoProperties properties;

        @Getter
        public static class KakaoAccount {
            private String email;

            @JsonProperty("profile")
            private KakaoProfile profile;

            @Getter
            public static class KakaoProfile {
                private String nickname;
            }
        }

        @Getter
        public static class KakaoProperties {
            private String nickname;
        }

        public String email() {
            return (kakaoAccount != null) ? kakaoAccount.email : null;
        }

        public String nicknameOrDefault() {
            if (properties != null && properties.nickname != null && !properties.nickname.isBlank()) return properties.nickname;
            if (kakaoAccount != null && kakaoAccount.profile != null && kakaoAccount.profile.nickname != null && !kakaoAccount.profile.nickname.isBlank())
                return kakaoAccount.profile.nickname;
            return "카카오사용자";
        }
    }
}
