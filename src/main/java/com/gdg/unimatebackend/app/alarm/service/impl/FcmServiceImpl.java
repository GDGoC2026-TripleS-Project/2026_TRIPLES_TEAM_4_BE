// ✅ FcmServiceImpl.java (풀버전)
// - "서버만으로 확인"을 위해: access token 발급 / 더미 전송 / 실제 전송 모두 제공
// - RestTemplate + HTTP v1 방식 유지

package com.gdg.unimatebackend.app.alarm.service.impl;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.gdg.unimatebackend.app.alarm.dto.FcmMessageDto;
import com.gdg.unimatebackend.app.alarm.dto.FcmSendDto;
import com.gdg.unimatebackend.app.alarm.service.FcmService;
import com.google.auth.oauth2.GoogleCredentials;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ClassPathResource;
import org.springframework.http.*;
import org.springframework.http.converter.StringHttpMessageConverter;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpStatusCodeException;
import org.springframework.web.client.RestTemplate;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.List;

@Slf4j
@Service
public class FcmServiceImpl implements FcmService {

    @Value("${fcm.project-id}")
    private String projectId;

    @Value("${fcm.key-path}")
    private String firebaseKeyPath;

    private final ObjectMapper objectMapper = new ObjectMapper();

    /**
     * ✅ 실제 전송: 프론트가 준 device token이 있을 때 사용
     * - 성공이면 "OK: <response-body>"
     * - 실패면 "ERROR: <status> <body>"
     */
    @Override
    public String sendMessageTo(FcmSendDto fcmSendDto) throws IOException {
        String jsonBody = makeMessage(fcmSendDto);
        return callFcmApi(jsonBody);
    }

    /**
     * ✅ 서버만으로 access token 발급 확인
     */
    @Override
    public String getAccessTokenForDebug() throws IOException {
        return getAccessTokenInternal();
    }

    /**
     * ✅ 서버만으로 FCM 호출 확인 (더미 토큰)
     * - 목적: 401/403인지, 400인지로 "서버 연동 상태" 판별
     *   - 400 INVALID_ARGUMENT -> 인증/권한 OK, 토큰만 없어서 실패 (정상적인 상태)
     *   - 401/403 -> 인증/권한/프로젝트ID/키 파일 문제
     */
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

    /**
     * ✅ FCM HTTP v1 호출 공통 로직
     */
    private String callFcmApi(String jsonBody) throws IOException {
        RestTemplate restTemplate = new RestTemplate();
        // 한글 깨짐 방지
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
            // FCM은 실패해도 body에 원인이 담기는 경우가 많음
            log.error("[FCM] status={} body={}", e.getStatusCode(), e.getResponseBodyAsString());
            return "ERROR: " + e.getStatusCode() + " " + e.getResponseBodyAsString();
        }
    }

    /**
     * ✅ OAuth2 Access Token 발급 (서비스 계정 키 사용)
     */
    private String getAccessTokenInternal() throws IOException {
        GoogleCredentials googleCredentials = GoogleCredentials
                .fromStream(new ClassPathResource(firebaseKeyPath).getInputStream())
                .createScoped(List.of("https://www.googleapis.com/auth/cloud-platform"));

        // 만료 시 갱신
        googleCredentials.refreshIfExpired();

        // refreshIfExpired 이후에도 null일 수 있어 강제 refresh 처리
        if (googleCredentials.getAccessToken() == null) {
            googleCredentials.refreshAccessToken();
        }

        return googleCredentials.getAccessToken().getTokenValue();
    }

    /**
     * ✅ DTO -> FCM HTTP v1 요청 바디(JSON) 생성
     */
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
