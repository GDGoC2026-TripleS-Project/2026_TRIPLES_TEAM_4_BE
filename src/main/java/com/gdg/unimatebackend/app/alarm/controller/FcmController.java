package com.gdg.unimatebackend.app.alarm.controller;

import com.gdg.unimatebackend.app.alarm.dto.FcmSendRequest;
import com.gdg.unimatebackend.app.alarm.dto.FcmTestResponse;
import com.gdg.unimatebackend.app.alarm.dto.FcmTokenRegisterRequest;
import com.gdg.unimatebackend.app.alarm.service.FcmTestService;
import com.gdg.unimatebackend.app.alarm.service.FcmTokenService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
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

    /* =========================
       FCM 토큰 등록
       ========================= */
    @Operation(
            summary = "내 FCM 토큰 등록/갱신",
            description =
                    """
                    ## 목적
                    현재 로그인한 사용자의 **FCM 디바이스 토큰을 서버(DB)에 저장/갱신**합니다.

                    ## 사용 순서(중요)
                    1) 모바일 앱에서 FCM 토큰을 발급받음  
                    2) 이 API(`/token/me`)로 토큰을 서버에 등록  
                    3) 등록이 끝난 뒤 `/test/me`로 실제 푸시 발송 테스트

                    ## 동작 개요
                    - 서버는 JWT 기반 인증을 통해 userId를 식별합니다.
                    - 동일 유저의 기존 토큰은 정책에 따라 비활성화될 수 있습니다.
                    """,
            security = @SecurityRequirement(name = "bearerAuth"),
            responses = {
                    @ApiResponse(responseCode = "200", description = "등록 성공"),
                    @ApiResponse(responseCode = "401", description = "인증 실패(토큰 누락/만료)")
            }
    )
    @PostMapping(value = "/token/me", consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<Void> registerMyToken(
            Authentication authentication,
            @Valid @RequestBody FcmTokenRegisterRequest request
    ) {
        Long userId = fcmTestService.extractUserId(authentication); // ✅ userId 추출 로직 단일화
        if (userId == null) {
            return ResponseEntity.status(401).build();
        }

        fcmTokenService.register(userId, request);
        return ResponseEntity.ok().build();
    }

    /* =========================
       내게 테스트 알림
       ========================= */
    @Operation(
            summary = "내 계정으로 푸시 테스트 발송",
            description =
                    """
                    ## 목적
                    현재 로그인한 사용자에게 **테스트 푸시를 즉시 발송**합니다.

                    ## 사전 조건(가장 중요)
                    - 반드시 먼저 `/api/v1/fcm/token/me` 를 호출해서 **FCM 토큰이 DB에 저장되어 있어야** 합니다.
                    - DB에 활성 토큰(`is_active = true`)이 없으면 404로 실패합니다.

                    ## 요청 Body(선택)
                    - title/body를 생략하면 기본 템플릿으로 발송됩니다.

                    ### 예시
                    ```json
                    {
                      "title": "now",
                      "body": "내용"
                    }
                    ```

                    ## 결과 해석
                    - success=true 이면 서버에서 FCM 호출 자체는 정상
                    - 단, **단말에서 배너(헤드업) 표시 여부는 알림 채널/OS 설정/앱 상태(포그라운드/백그라운드)**에 따라 달라질 수 있습니다.
                    """,
            security = @SecurityRequirement(name = "bearerAuth"),
            responses = {
                    @ApiResponse(responseCode = "200", description = "FCM 발송 시도 완료(성공/실패 상세는 body 참고)",
                            content = @Content(schema = @Schema(implementation = FcmTestResponse.class))),
                    @ApiResponse(responseCode = "401", description = "인증 실패(토큰 누락/만료)",
                            content = @Content(schema = @Schema(implementation = FcmTestResponse.class))),
                    @ApiResponse(responseCode = "404", description = "활성 FCM 토큰 없음",
                            content = @Content(schema = @Schema(implementation = FcmTestResponse.class)))
            }
    )
    @PostMapping(value = "/test/me", consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
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
        if (userId == null) {
            return ResponseEntity.status(401).body(
                    FcmTestResponse.builder()
                            .success(false)
                            .message("Unauthorized")
                            .detail("Cannot extract userId from authentication")
                            .build()
            );
        }

        FcmTestResponse response = fcmTestService.sendTestToUser(userId, request);
        // 선택: status를 response.success에 맞춰 조절하고 싶으면 여기서 분기 가능
        return ResponseEntity.ok(response);
    }
}
