// ✅ FcmController.java (서버만으로 검증 가능한 디버그 엔드포인트 포함 "풀버전")
// package 경로는 네 프로젝트에 맞춰 그대로 유지

package com.gdg.unimatebackend.app.alarm.controller;

import com.gdg.unimatebackend.app.alarm.dto.FcmSendDto;
import com.gdg.unimatebackend.app.alarm.service.FcmService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;

@Slf4j
@RestController
@RequestMapping("/api/v1/fcm")
@RequiredArgsConstructor
public class FcmController {

    private final FcmService fcmService;

    /**
     * ✅ 1) 서버만으로 "키 파일 로딩 + OAuth AccessToken 발급" 확인
     * - 프론트 토큰 없어도 됨
     */
    @GetMapping("/debug/access-token")
    public ResponseEntity<String> debugAccessToken() throws IOException {
        String accessToken = fcmService.getAccessTokenForDebug();

        // 로그에는 마스킹 유지 (좋은 습관)
        String masked = accessToken.length() > 20 ? accessToken.substring(0, 20) + "..." : accessToken;
        log.info("[FCM DEBUG] accessToken(masked)={}", masked);

        // 응답은 전체 반환
        return ResponseEntity.ok(accessToken);
    }

    /**
     * ✅ 2) 서버만으로 "FCM HTTP v1 호출"이 되는지 확인
     * - token이 가짜여도 됨 (그 경우 400 INVALID_ARGUMENT이 정상적인 결과)
     * - 401/403이면 인증/권한/프로젝트ID/키파일 문제가 있는 것
     */
    @PostMapping("/debug/send-dummy")
    public ResponseEntity<String> sendDummy() throws IOException {
        String result = fcmService.sendDummyMessageForDebug();
        return ResponseEntity.ok(result);
    }

    /**
     * ✅ 3) 실제 전송 API (토큰이 있을 때)
     * - Swagger에서 body로 token/title/body 넣어서 호출
     */
    @PostMapping("/debug/send")
    public ResponseEntity<String> send(@RequestBody @Valid FcmSendDto dto) throws IOException {
        String result = fcmService.sendMessageTo(dto);
        return ResponseEntity.ok(result);
    }
}
