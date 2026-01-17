package com.gdg.unimatebackend.app.auth.service;

import com.gdg.unimatebackend.app.auth.dto.NaverTokenResponse;
import com.gdg.unimatebackend.app.auth.dto.NaverUserInfo;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

@Service
@Slf4j
public class NaverApiService {

    private final RestTemplate restTemplate = new RestTemplate();

    @Value("${oauth.naver.token-uri}")
    private String tokenUri;

    @Value("${oauth.naver.user-info-uri}")
    private String userInfoUri;

    @Value("${oauth.naver.client-id}")
    private String clientId;

    @Value("${oauth.naver.client-secret}")
    private String clientSecret;

    public NaverTokenResponse exchangeCodeToToken(String code, String state, String redirectUri) {
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

    public NaverUserInfo getUserInfo(String accessToken) {
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(accessToken);

        ResponseEntity<NaverUserInfo> res = restTemplate.exchange(
                userInfoUri, HttpMethod.GET, new HttpEntity<>(headers), NaverUserInfo.class
        );

        return res.getBody();
    }
}
