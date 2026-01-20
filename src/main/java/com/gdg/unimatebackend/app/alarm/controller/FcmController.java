package com.gdg.unimatebackend.app.alarm.controller;

import com.gdg.unimatebackend.app.alarm.dto.FcmSendDto;
import com.gdg.unimatebackend.app.alarm.dto.FcmTestSendRequest;
import com.gdg.unimatebackend.app.alarm.dto.FcmTokenRegisterRequest;
import com.gdg.unimatebackend.app.alarm.entity.FcmDeviceToken;
import com.gdg.unimatebackend.app.alarm.repository.FcmDeviceTokenRepository;
import com.gdg.unimatebackend.app.alarm.service.FcmService;
import com.gdg.unimatebackend.app.alarm.service.FcmTokenService;
import com.gdg.unimatebackend.app.alarm.support.UserIdResolver;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;

@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/fcm")
public class FcmController {

    private final FcmService fcmService;
    private final FcmTokenService fcmTokenService;
    private final FcmDeviceTokenRepository tokenRepository;
    private final UserIdResolver userIdResolver;

    @PostMapping(value = "/token", consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<Void> registerToken(
            @RequestHeader(value = "X-USER-ID", required = false) String headerUserId,
            @Valid @RequestBody FcmTokenRegisterRequest request
    ) {
        Long userId = userIdResolver.resolveOrThrow(headerUserId);
        fcmTokenService.register(userId, request);
        return ResponseEntity.ok().build();
    }

    /**
     * ✅ 서버 단독: 내 저장된 토큰으로 템플릿 발송
     * - body/title 없으면 기본값
     * - consumes 제거해서 body 없이도 호출 가능하게
     */
    @PostMapping(value = "/test/me", produces = MediaType.TEXT_PLAIN_VALUE)
    public ResponseEntity<String> testSendToMe(
            @RequestHeader(value = "X-USER-ID", required = false) String headerUserId,
            @RequestBody(required = false) FcmTestSendRequest request
    ) throws IOException {

        Long userId = userIdResolver.resolveOrThrow(headerUserId);

        FcmDeviceToken token = tokenRepository.findTopByUserIdAndIsActiveTrueOrderByUpdatedAtDesc(userId)
                .orElseThrow(() -> new IllegalStateException("No active FCM token for userId=" + userId));

        String title = (request == null || request.getTitle() == null || request.getTitle().isBlank())
                ? "Unimate Test"
                : request.getTitle();

        String body = (request == null || request.getBody() == null || request.getBody().isBlank())
                ? "서버 템플릿 발송 테스트"
                : request.getBody();

        String result = fcmService.sendMessageTo(FcmSendDto.builder()
                .token(token.getToken())
                .title(title)
                .body(body)
                .build());

        return ResponseEntity.ok(result);
    }

    @GetMapping(value = "/debug/access-token", produces = MediaType.TEXT_PLAIN_VALUE)
    public ResponseEntity<String> debugAccessToken() throws IOException {
        String accessToken = fcmService.getAccessTokenForDebug();
        return ResponseEntity.ok(accessToken);
    }

    @PostMapping(value = "/debug/send-dummy", produces = MediaType.TEXT_PLAIN_VALUE)
    public ResponseEntity<String> sendDummy() throws IOException {
        String result = fcmService.sendDummyMessageForDebug();
        return ResponseEntity.ok(result);
    }

    @PostMapping(value = "/debug/send", consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.TEXT_PLAIN_VALUE)
    public ResponseEntity<String> send(@RequestBody FcmSendDto dto) throws IOException {
        String result = fcmService.sendMessageTo(dto);
        return ResponseEntity.ok(result);
    }
}
