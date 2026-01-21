package com.gdg.unimatebackend.app.alarm.controller;

import com.gdg.unimatebackend.app.alarm.dto.FcmSendDto;
import com.gdg.unimatebackend.app.alarm.dto.FcmSendRequest;
import com.gdg.unimatebackend.app.alarm.dto.FcmTokenRegisterRequest;
import com.gdg.unimatebackend.app.alarm.entity.FcmDeviceToken;
import com.gdg.unimatebackend.app.alarm.repository.FcmDeviceTokenRepository;
import com.gdg.unimatebackend.app.alarm.service.FcmService;
import com.gdg.unimatebackend.app.alarm.service.FcmTokenService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.util.Map;

@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/fcm")
public class FcmController {

    private final FcmService fcmService;
    private final FcmTokenService fcmTokenService;
    private final FcmDeviceTokenRepository tokenRepository;

    /* =========================
       FCM 토큰 등록
       ========================= */
    @PostMapping(value = "/token/me", consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<Void> registerMyToken(
            Authentication authentication,
            @Valid @RequestBody FcmTokenRegisterRequest request
    ) {
        Long userId = extractUserId(authentication);
        if (userId == null) {
            return ResponseEntity.status(401).build();
        }

        fcmTokenService.register(userId, request);
        return ResponseEntity.ok().build();
    }

    /* =========================
       내게 테스트 알림
       ========================= */
    @PostMapping(value = "/test/me", produces = MediaType.TEXT_PLAIN_VALUE)
    public ResponseEntity<String> testSendToMe(
            Authentication authentication,
            @RequestBody(required = false) FcmSendRequest request
    ) throws IOException {

        Long userId = extractUserId(authentication);
        if (userId == null) {
            return ResponseEntity.status(401).body("Invalid principal");
        }

        FcmDeviceToken token = tokenRepository
                .findTopByUserIdAndIsActiveTrueOrderByUpdatedAtDesc(userId)
                .orElse(null);

        if (token == null) {
            return ResponseEntity.status(404).body("No active FCM token");
        }

        String title = (request == null || request.getTitle() == null || request.getTitle().isBlank())
                ? "Unimate Test"
                : request.getTitle();

        String body = (request == null || request.getBody() == null || request.getBody().isBlank())
                ? "서버 템플릿 발송 테스트"
                : request.getBody();

        String result = fcmService.sendMessageTo(
                FcmSendDto.builder()
                        .token(token.getToken())
                        .title(title)
                        .body(body)
                        .build()
        );

        return ResponseEntity.ok(result);
    }

    /* =========================
       userId 추출 (단 하나의 정답)
       ========================= */
    private Long extractUserId(Authentication authentication) {
        if (authentication == null) return null;

        // 1️⃣ name()에 숫자
        try {
            String name = authentication.getName();
            if (name != null && name.matches("\\d+")) {
                return Long.parseLong(name);
            }
        } catch (Exception ignored) {}

        Object principal = authentication.getPrincipal();
        if (principal == null) return null;

        // 2️⃣ principal이 String
        if (principal instanceof String s && s.matches("\\d+")) {
            return Long.parseLong(s);
        }

        // 3️⃣ JWT claims(Map)
        if (principal instanceof Map<?, ?> map) {
            Object sub = map.get("sub");
            if (sub != null && sub.toString().matches("\\d+")) {
                return Long.parseLong(sub.toString());
            }
        }

        // 4️⃣ 커스텀 Principal
        for (String method : new String[]{"getUserId", "getId"}) {
            try {
                Object v = principal.getClass().getMethod(method).invoke(principal);
                if (v != null && v.toString().matches("\\d+")) {
                    return Long.parseLong(v.toString());
                }
            } catch (Exception ignored) {}
        }

        return null;
    }
}
