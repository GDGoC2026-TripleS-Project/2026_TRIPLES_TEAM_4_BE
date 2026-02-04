package com.gdg.unimatebackend.app.alarm.service.impl;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.gdg.unimatebackend.app.alarm.dto.FcmSendDto;
import com.gdg.unimatebackend.app.alarm.service.FcmService;
import com.google.auth.oauth2.AccessToken;
import com.google.auth.oauth2.GoogleCredentials;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;
import org.springframework.http.*;
import org.springframework.http.converter.StringHttpMessageConverter;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpStatusCodeException;
import org.springframework.web.client.RestTemplate;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
public class FcmServiceImpl implements FcmService {

    @Value("${fcm.project-id:}")
    private String projectId;

    /**
     * 권장:
     * - classpath:firebase/unimate.json
     * - file:/app/firebase-key.json
     *
     * 실수 방지:
     * - "/app/firebase-key.json" 으로 들어오면 자동으로 "file:" 붙여서 읽는다
     */
    @Value("${fcm.key-path:}")
    private String firebaseKeyPath;

    private final ResourceLoader resourceLoader;
    private final ObjectMapper objectMapper = new ObjectMapper();

    // ✅ AccessToken 캐시
    private volatile String cachedAccessToken;
    private volatile Instant cachedAccessTokenExpireAt;

    public FcmServiceImpl(ResourceLoader resourceLoader) {
        this.resourceLoader = resourceLoader;
    }

    @Override
    public String sendMessageTo(FcmSendDto dto) throws IOException {
        String jsonBody = makeMessage(dto);
        return callFcmApi(jsonBody);
    }

    @Override
    public String getAccessTokenForDebug() throws IOException {
        return getAccessTokenInternal();
    }

    @Override
    public String sendDummyMessageForDebug() throws IOException {
        FcmSendDto dummy = FcmSendDto.builder()
                .token("DUMMY_TOKEN_TO_CHECK_SERVER_ONLY")
                .title("Unimate Dummy Test")
                .body("This is dummy message to validate server-side FCM integration.")
                .build();

        String jsonBody = makeMessage(dummy);
        return callFcmApi(jsonBody);
    }

    private String callFcmApi(String jsonBody) throws IOException {
        RestTemplate restTemplate = new RestTemplate();
        restTemplate.getMessageConverters().add(0, new StringHttpMessageConverter(StandardCharsets.UTF_8));

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setBearerAuth(getAccessTokenInternal());

        HttpEntity<String> entity = new HttpEntity<>(jsonBody, headers);
        String apiUrl = "https://fcm.googleapis.com/v1/projects/" + projectId + "/messages:send";

        try {
            ResponseEntity<String> response = restTemplate.exchange(apiUrl, HttpMethod.POST, entity, String.class);
            log.info("[FCM] send ok. status={}", response.getStatusCode());
            return "OK: " + response.getBody();
        } catch (HttpStatusCodeException e) {
            log.error("[FCM] send fail. status={}, body={}", e.getStatusCode(), e.getResponseBodyAsString());
            return "ERROR: " + e.getStatusCode() + " " + e.getResponseBodyAsString();
        }
    }

    private String getAccessTokenInternal() throws IOException {
        // 0) 캐시 유효하면 재사용 (만료 60초 전부터 갱신)
        if (cachedAccessToken != null && cachedAccessTokenExpireAt != null) {
            if (cachedAccessTokenExpireAt.isAfter(Instant.now().plusSeconds(60))) {
                return cachedAccessToken;
            }
        }

        String keyPath = normalizeKeyPath(firebaseKeyPath);

        if (projectId == null || projectId.isBlank()) {
            throw new IllegalStateException("fcm.project-id is blank");
        }
        if (keyPath == null || keyPath.isBlank()) {
            throw new IllegalStateException("fcm.key-path is blank");
        }

        // ✅ 이 로그 한 줄로 운영에서 99% 원인 추적 끝남
        log.info("[FCM] resolving key. projectId={}, keyPath={}", projectId, keyPath);

        Resource resource = resourceLoader.getResource(keyPath);
        if (!resource.exists()) {
            throw new IllegalStateException("FCM key resource not found. fcm.key-path=" + keyPath);
        }

        GoogleCredentials credentials = GoogleCredentials
                .fromStream(resource.getInputStream())
                .createScoped(List.of("https://www.googleapis.com/auth/cloud-platform"));

        credentials.refreshIfExpired();
        if (credentials.getAccessToken() == null) {
            credentials.refreshAccessToken();
        }

        AccessToken accessToken = credentials.getAccessToken();
        if (accessToken == null || accessToken.getTokenValue() == null) {
            throw new IllegalStateException("Failed to obtain FCM access token (null)");
        }

        cachedAccessToken = accessToken.getTokenValue();
        cachedAccessTokenExpireAt = (accessToken.getExpirationTime() != null)
                ? accessToken.getExpirationTime().toInstant()
                : Instant.now().plusSeconds(300);

        return cachedAccessToken;
    }

    private String normalizeKeyPath(String path) {
        if (path == null) return null;
        String p = path.trim();
        if (p.isEmpty()) return p;

        // 이미 스킴이 있으면 그대로
        if (p.startsWith("classpath:") || p.startsWith("file:")) return p;

        // "/app/..." 같이 절대경로면 file: 강제
        if (p.startsWith("/")) return "file:" + p;

        return p;
    }

    private String makeMessage(FcmSendDto dto) throws JsonProcessingException {
        Map<String, Object> root = new HashMap<>();
        Map<String, Object> message = new HashMap<>();

        message.put("token", dto.getToken());

        Map<String, String> notification = new HashMap<>();
        notification.put("title", dto.getTitle());
        notification.put("body", dto.getBody());
        message.put("notification", notification);

        Map<String, String> data = new HashMap<>();
        if (dto.getData() != null && !dto.getData().isEmpty()) {
            data.putAll(dto.getData());
        } else {
            data.put("type", "TEST");
            data.put("sentAt", Instant.now().toString());
        }
        message.put("data", data);

        Map<String, Object> android = new HashMap<>();
        android.put("priority", "HIGH");
        message.put("android", android);

        root.put("message", message);
        return objectMapper.writeValueAsString(root);
    }
}
