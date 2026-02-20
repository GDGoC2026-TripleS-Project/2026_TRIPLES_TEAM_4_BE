package com.gdg.unimatebackend.auth.service;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

@Service
@RequiredArgsConstructor
public class GoogleApiService {

    private final RestTemplate restTemplate;

    @Value("${oauth.google.client-id:}")
    private String clientId;

    @Value("${oauth.google.token-info-uri:https://oauth2.googleapis.com/tokeninfo}")
    private String tokenInfoUri;

    public GoogleUserInfo fetchUserInfoFromIdToken(String idToken) {
        try {
            String uri = UriComponentsBuilder.fromUriString(tokenInfoUri)
                    .queryParam("id_token", idToken)
                    .toUriString();

            ResponseEntity<GoogleTokenInfoResponse> response = restTemplate.exchange(
                    uri,
                    HttpMethod.GET,
                    null,
                    GoogleTokenInfoResponse.class
            );

            GoogleTokenInfoResponse body = response.getBody();
            if (body == null || body.sub == null || body.sub.isBlank()) {
                throw new IllegalArgumentException("GOOGLE_USER_INFO_FETCH_FAILED");
            }

            if (clientId != null && !clientId.isBlank() && !clientId.equals(body.aud)) {
                throw new IllegalArgumentException("GOOGLE_AUDIENCE_MISMATCH");
            }

            return new GoogleUserInfo(
                    body.sub,
                    body.email,
                    (body.name == null || body.name.isBlank()) ? "구글사용자" : body.name
            );
        } catch (HttpClientErrorException e) {
            throw new IllegalArgumentException("GOOGLE_TOKEN_INVALID");
        }
    }

    @Getter
    public static class GoogleTokenInfoResponse {
        private String sub;
        private String email;
        private String name;
        private String aud;
    }

    public record GoogleUserInfo(String id, String email, String name) {}
}
