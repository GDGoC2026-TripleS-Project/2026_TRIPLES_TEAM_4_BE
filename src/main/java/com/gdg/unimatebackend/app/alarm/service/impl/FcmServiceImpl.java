package com.gdg.unimatebackend.app.alarm.service.impl;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
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
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
public class FcmServiceImpl implements FcmService {

    @Value("${fcm.project-id}")
    private String projectId;

    /**
     * 예) file:/app/firebase-key.json
     */
    @Value("${fcm.key-path}")
    private String firebaseKeyPath;

    private final ResourceLoader resourceLoader;
    private final ObjectMapper objectMapper = new ObjectMapper();

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
            log.info("[FCM] status={}, body={}", response.getStatusCode(), response.getBody());
            return "OK: " + response.getBody();
        } catch (HttpStatusCodeException e) {
            log.error("[FCM] status={} body={}", e.getStatusCode(), e.getResponseBodyAsString());
            return "ERROR: " + e.getStatusCode() + " " + e.getResponseBodyAsString();
        }
    }

    private String getAccessTokenInternal() throws IOException {
        Resource resource = resourceLoader.getResource(firebaseKeyPath);

        GoogleCredentials credentials = GoogleCredentials
                .fromStream(resource.getInputStream())
                .createScoped(List.of("https://www.googleapis.com/auth/cloud-platform"));

        credentials.refreshIfExpired();
        if (credentials.getAccessToken() == null) {
            credentials.refreshAccessToken();
        }

        return credentials.getAccessToken().getTokenValue();
    }

    /**
     * ✅ notification + data + android priority HIGH
     */
    private String makeMessage(FcmSendDto dto) throws JsonProcessingException {
        Map<String, Object> root = new HashMap<>();
        Map<String, Object> message = new HashMap<>();

        message.put("token", dto.getToken());

        // notification (배경에서 OS가 자동으로 띄움)
        Map<String, String> notification = new HashMap<>();
        notification.put("title", dto.getTitle());
        notification.put("body", dto.getBody());
        message.put("notification", notification);

        // data (포그라운드/딥링크용 - 지금은 테스트 값)
        Map<String, String> data = new HashMap<>();
        data.put("type", "TEST");
        data.put("sentAt", Instant.now().toString());
        message.put("data", data);

        // android priority
        Map<String, Object> android = new HashMap<>();
        android.put("priority", "HIGH");
        message.put("android", android);

        root.put("message", message);

        return objectMapper.writeValueAsString(root);
    }
}
