package com.gdg.unimatebackend.alarm.controller;

import com.gdg.unimatebackend.alarm.dto.FcmSendRequest;
import com.gdg.unimatebackend.alarm.dto.FcmTestResponse;
import com.gdg.unimatebackend.alarm.dto.FcmTokenRegisterRequest;
import com.gdg.unimatebackend.alarm.service.FcmTestService;
import com.gdg.unimatebackend.alarm.service.FcmTokenService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/fcm")
@Tag(name = "FCM", description = "FCM 토큰 등록 및 테스트 발송 API")
public class FcmController {

    private final FcmTokenService fcmTokenService;
    private final FcmTestService fcmTestService;

    @Operation(
            summary = "내 FCM 토큰 등록/갱신",
            description = """
                    ## 목적
                    로그인한 사용자의 **FCM 토큰을 DB에 등록/갱신**합니다.

                    ## 디바이스 식별 정책(서버 기준)
                    - 등록키: (user_id, device_id, platform)
                    - 같은 디바이스에서 토큰이 바뀌면 UPDATE
                    - 다른 디바이스면 INSERT
                    - (테스트 A안) 유저당 활성 1개만 유지 → 마지막으로 등록한 디바이스만 수신

                    ## 테스트 순서
                    1) /api/v1/fcm/token/me 로 토큰 등록
                    2) /api/v1/fcm/test/me 로 푸시 발송

                    ## 요청 예시
                    ```json
                    {
                      "token": "fcmToken...",
                      "deviceId": "pixel4-uuid-or-android-id",
                      "platform": "ANDROID"
                    }
                    ```
                    """,
            security = @SecurityRequirement(name = "bearerAuth"),
            responses = {
                    @ApiResponse(responseCode = "200", description = "등록 성공"),
                    @ApiResponse(responseCode = "401", description = "인증 실패")
            }
    )
    @PostMapping(value = "/token/me", consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<Void> registerMyToken(
            Authentication authentication,
            @Valid @RequestBody FcmTokenRegisterRequest request
    ) {
        Long userId = fcmTestService.extractUserId(authentication);
        if (userId == null || userId <= 0) {
            return ResponseEntity.status(401).build();
        }

        fcmTokenService.register(userId, request);
        return ResponseEntity.ok().build();
    }

    @Operation(
            summary = "내 계정으로 푸시 테스트 발송",
            description = """
                    ## 목적
                    현재 로그인한 사용자에게 테스트 푸시를 발송합니다.

                    ## 사전 조건
                    - 먼저 /api/v1/fcm/token/me 로 토큰 등록이 되어 있어야 합니다.
                    - 활성 토큰이 없으면 success=false로 내려갑니다.

                    ## 요청 Body(선택)
                    ```json
                    {
                      "title": "now",
                      "body": "내용"
                    }
                    ```
                    """,
            security = @SecurityRequirement(name = "bearerAuth"),
            responses = {
                    @ApiResponse(responseCode = "200", description = "발송 시도 결과 반환"),
                    @ApiResponse(responseCode = "401", description = "인증 실패")
            }
    )
    @PostMapping(value = "/test/me", consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<FcmTestResponse> testMe(
            Authentication authentication,
            @RequestBody(required = false) FcmSendRequest request
    ) {
        Long userId = fcmTestService.extractUserId(authentication);
        if (userId == null || userId <= 0) {
            return ResponseEntity.status(401).body(
                    FcmTestResponse.builder()
                            .success(false)
                            .message("Unauthorized")
                            .detail("Cannot extract userId from authentication")
                            .build()
            );
        }

        return ResponseEntity.ok(fcmTestService.sendTestToUser(userId, request));
    }
}
