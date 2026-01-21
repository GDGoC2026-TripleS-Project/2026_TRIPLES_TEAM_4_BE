package com.gdg.unimatebackend.app.alarm.controller;

import com.gdg.unimatebackend.app.alarm.dto.FcmSendRequest;
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
    public ResponseEntity<FcmTestResponse> testMe(
            Authentication authentication,
            @RequestBody(required = false) FcmSendRequest request
    ) {
        if (authentication == null) {
            return ResponseEntity.status(401).body(
                    FcmTestResponse.builder()
                            .success(false)
                            .message("Unauthorized")
                            .detail("Authentication is null")
                            .build()
            );
        }

        Long userId = fcmTestService.extractUserId(authentication);

        return ResponseEntity.ok(
                fcmTestService.sendTestToUser(userId, request)
        );
    }
}
