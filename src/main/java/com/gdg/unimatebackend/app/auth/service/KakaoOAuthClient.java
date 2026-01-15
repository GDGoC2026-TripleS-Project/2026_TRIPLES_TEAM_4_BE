package com.gdg.unimatebackend.app.auth.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.reactive.function.BodyInserters;
import org.springframework.web.reactive.function.client.WebClient;

@Component
@RequiredArgsConstructor
public class KakaoOAuthClient {

    private final ObjectMapper objectMapper = new ObjectMapper();
    private final WebClient webClient = WebClient.builder().build();

    @Value("${oauth.kakao.client-id:}")
    private String clientId;

    @Value("${oauth.kakao.client-secret:}")
    private String clientSecret;

    @Value("${oauth.kakao.redirect-uri:}")
    private String defaultRedirectUri;

    @Value("${oauth.kakao.token-uri:https://kauth.kakao.com/oauth/token}")
    private String tokenUri;

    @Value("${oauth.kakao.user-info-uri:https://kapi.kakao.com/v2/user/me}")
    private String userInfoUri;

    public String exchangeCodeToAccessToken(String code, String redirectUri) {
        String ru = (redirectUri != null && !redirectUri.isBlank()) ? redirectUri : defaultRedirectUri;

        if (clientId == null || clientId.isBlank()) {
            throw new IllegalArgumentException("KAKAO_CLIENT_ID is required.");
        }
        if (ru == null || ru.isBlank()) {
            throw new IllegalArgumentException("KAKAO_REDIRECT_URI is required for code login.");
        }

        MultiValueMap<String, String> form = new LinkedMultiValueMap<>();
        form.add("grant_type", "authorization_code");
        form.add("client_id", clientId);
        form.add("redirect_uri", ru);
        form.add("code", code);

        if (clientSecret != null && !clientSecret.isBlank()) {
            form.add("client_secret", clientSecret);
        }

        String body = webClient.post()
                .uri(tokenUri)
                .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                .accept(MediaType.APPLICATION_JSON)
                .body(BodyInserters.fromFormData(form))
                .retrieve()
                .bodyToMono(String.class)
                .block();

        try {
            JsonNode node = objectMapper.readTree(body);
            JsonNode accessToken = node.get("access_token");
            if (accessToken == null) throw new IllegalArgumentException("Kakao token response missing access_token.");
            return accessToken.asText();
        } catch (Exception e) {
            throw new IllegalArgumentException("Failed to parse kakao token response.");
        }
    }

    public KakaoUserInfo fetchUserInfo(String accessToken) {
        String body = webClient.get()
                .uri(userInfoUri)
                .accept(MediaType.APPLICATION_JSON)
                .header("Authorization", "Bearer " + accessToken)
                .retrieve()
                .bodyToMono(String.class)
                .block();

        try {
            JsonNode root = objectMapper.readTree(body);

            String id = root.get("id").asText();

            JsonNode kakaoAccount = root.get("kakao_account");
            String email = null;
            if (kakaoAccount != null && kakaoAccount.get("email") != null) {
                email = kakaoAccount.get("email").asText();
            }

            String nickname = null;
            JsonNode profile = (kakaoAccount != null) ? kakaoAccount.get("profile") : null;
            if (profile != null && profile.get("nickname") != null) {
                nickname = profile.get("nickname").asText();
            }

            return new KakaoUserInfo(id, email, nickname);
        } catch (Exception e) {
            throw new IllegalArgumentException("Failed to parse kakao user info.");
        }
    }

    @Getter
    public static class KakaoUserInfo {
        private final String providerId;
        private final String email;
        private final String nickname;

        public KakaoUserInfo(String providerId, String email, String nickname) {
            this.providerId = providerId;
            this.email = email;
            this.nickname = nickname;
        }
    }
}
