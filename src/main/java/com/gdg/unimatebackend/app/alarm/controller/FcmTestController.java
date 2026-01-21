package com.gdg.unimatebackend.app.alarm.controller;

import com.gdg.unimatebackend.app.alarm.dto.FcmSendDto;
import com.gdg.unimatebackend.app.alarm.dto.FcmTestResponse;
import com.gdg.unimatebackend.app.alarm.service.FcmTestService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/fcm")
@RequiredArgsConstructor
public class FcmTestController {

    private final FcmTestService fcmTestService;

    @PostMapping("/test/me")
    public ResponseEntity<FcmTestResponse> testMe(Authentication authentication) {
        // ✅ 여기서 NPE 나면 500이 아니라 401/403으로 떨어져야 정상인데,
        // 혹시라도 null이면 안전하게 처리
        if (authentication == null || authentication.getPrincipal() == null) {
            return ResponseEntity.status(401).body(
                    FcmTestResponse.builder()
                            .success(false)
                            .message("Unauthorized")
                            .detail("Authentication is null")
                            .build()
            );
        }
        System.out.println("principalClass=" + authentication.getPrincipal().getClass().getName()
                + ", name=" + authentication.getName()
                + ", principal=" + authentication.getPrincipal());


        // ✅ 너 프로젝트 principal 구조에 맞게 “userId 추출”만 맞추면 됨
        Long userId = fcmTestService.extractUserId(authentication);

        return ResponseEntity.ok(
                fcmTestService.sendTestToUser(userId)
        );
    }
}
