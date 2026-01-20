package com.gdg.unimatebackend.app.alarm.service.impl;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.gdg.unimatebackend.app.alarm.dto.FcmMessageDto;
import com.gdg.unimatebackend.app.alarm.dto.FcmSendDto;
import com.gdg.unimatebackend.app.alarm.service.FcmService;
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
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;

@Slf4j
@Service
public class FcmServiceImpl implements FcmService {

    @Value("${fcm.project-id}")
    private String projectId;

    /**
     * 예시
     *  - 운영(도커 마운트): /app/firebase-key.json   또는 file:/app/firebase-key.json
     *  - 로컬(classpath):  classpath:firebase/unimate.json
     */
    @Value("${fcm.key-path}")
    private String firebaseKeyPath;

    private final ResourceLoader resourceLoader;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public FcmServiceImpl(ResourceLoader resourceLoader) {
        this.resourceLoader = resourceLoader;
    }

    @Override
    public String sendMessageTo(FcmSendDto fcmSendDto) throws IOException {
        String jsonBody = makeMessage(fcmSendDto);
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
            log.info("[FCM] status={}", response.getStatusCode());
            log.info("[FCM] body={}", response.getBody());
            return "OK: " + response.getBody();
        } catch (HttpStatusCodeException e) {
            log.error("[FCM] status={} body={}", e.getStatusCode(), e.getResponseBodyAsString());
            return "ERROR: " + e.getStatusCode() + " " + e.getResponseBodyAsString();
        }
    }

    /**
     * ✅ OAuth2 Access Token 발급 (classpath/file 모두 지원)
     */
    private String getAccessTokenInternal() throws IOException {
        Resource resource = resolveKeyResource(firebaseKeyPath);

        try (InputStream is = resource.getInputStream()) {
            GoogleCredentials credentials = GoogleCredentials.fromStream(is)
                    // ✅ FCM HTTP v1 최소 권한 스코프
                    .createScoped(List.of("https://www.googleapis.com/auth/firebase.messaging"));

            credentials.refreshIfExpired();

            if (credentials.getAccessToken() == null) {
                credentials.refreshAccessToken();
            }

            return credentials.getAccessToken().getTokenValue();
        }
    }

    /**
     * firebaseKeyPath가 classpath 접두어가 없고, "/"로 시작하면 file로 간주해준다.
     */
    private Resource resolveKeyResource(String path) {
        if (path == null || path.isBlank()) {
            throw new IllegalStateException("fcm.key-path is blank");
        }

        String trimmed = path.trim();

        // classpath:/ file:/ http: 같은 스킴이 이미 있으면 그대로 로딩
        if (trimmed.contains(":")) {
            return resourceLoader.getResource(trimmed);
        }

        // "/app/firebase-key.json" 같은 절대경로면 file로 처리
        if (trimmed.startsWith("/")) {
            return resourceLoader.getResource("file:" + trimmed);
        }

        // 그 외는 classpath로 처리 (예: "firebase/unimate.json")
        return resourceLoader.getResource("classpath:" + trimmed);
    }

    private String makeMessage(FcmSendDto fcmSendDto) throws JsonProcessingException {
        FcmMessageDto payload = FcmMessageDto.builder()
                .validateOnly(false)
                .message(FcmMessageDto.Message.builder()
                        .token(fcmSendDto.getToken())
                        .notification(FcmMessageDto.Notification.builder()
                                .title(fcmSendDto.getTitle())
                                .body(fcmSendDto.getBody())
                                .image(null)
                                .build())
                        .build())
                .build();

        return objectMapper.writeValueAsString(payload);
    }
}
