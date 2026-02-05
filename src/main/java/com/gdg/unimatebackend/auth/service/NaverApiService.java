package com.gdg.unimatebackend.auth.service;

import com.gdg.unimatebackend.auth.dto.NaverTokenResponse;
import com.gdg.unimatebackend.auth.dto.NaverUserInfo;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

@Service
@RequiredArgsConstructor
public class NaverApiService {

    private final RestTemplate restTemplate;

    @Value("${oauth.naver.client-id}")
    private String clientId;

    @Value("${oauth.naver.client-secret}")
    private String clientSecret;

    @Value("${oauth.naver.authorize-uri:https://nid.naver.com/oauth2.0/authorize}")
    private String authorizeUri;

    @Value("${oauth.naver.token-uri:https://nid.naver.com/oauth2.0/token}")
    private String tokenUri;

    @Value("${oauth.naver.user-info-uri:https://openapi.naver.com/v1/nid/me}")
    private String userInfoUri;

    @Value("${oauth.naver.redirect-uri}")
    private String redirectUri;

    public String buildAuthorizeUrl(String state) {
        return UriComponentsBuilder.fromUriString(authorizeUri)
                .queryParam("response_type", "code")
                .queryParam("client_id", clientId)
                .queryParam("redirect_uri", redirectUri)
                .queryParam("state", state)
                .toUriString();
    }

    public NaverTokenResponse exchangeCodeForToken(String code, String state) {
        String url = UriComponentsBuilder.fromUriString(tokenUri)
                .queryParam("grant_type", "authorization_code")
                .queryParam("client_id", clientId)
                .queryParam("client_secret", clientSecret)
                .queryParam("code", code)
                .queryParam("state", state)
                .queryParam("redirect_uri", redirectUri)
                .toUriString();

        ResponseEntity<NaverTokenResponse> res = restTemplate.exchange(
                url, HttpMethod.GET, new HttpEntity<>(new HttpHeaders()), NaverTokenResponse.class
        );
        return res.getBody();
    }

    public NaverUserInfo fetchUserInfo(String accessToken) {
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(accessToken);

        ResponseEntity<NaverUserInfo> res = restTemplate.exchange(
                userInfoUri, HttpMethod.GET, new HttpEntity<>(headers), NaverUserInfo.class
        );

        return res.getBody();
    }

    public String getRedirectUri() {
        return redirectUri;
    }
}
