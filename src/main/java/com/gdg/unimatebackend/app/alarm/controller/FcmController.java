package com.gdg.unimatebackend.app.alarm.controller;

import com.gdg.unimatebackend.app.alarm.dto.FcmSendDto;
import com.gdg.unimatebackend.app.alarm.dto.FcmSendRequest;
import com.gdg.unimatebackend.app.alarm.dto.FcmTokenRegisterRequest;
import com.gdg.unimatebackend.app.alarm.entity.FcmDeviceToken;
import com.gdg.unimatebackend.app.alarm.repository.FcmDeviceTokenRepository;
import com.gdg.unimatebackend.app.alarm.service.FcmService;
import com.gdg.unimatebackend.app.alarm.service.FcmTokenService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
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

import java.io.IOException;

@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/fcm")
@Tag(
        name = "FCM 알림",
        description = """
                Firebase Cloud Messaging(FCM) 기반 푸시 알림 API
                
                ✅ Swagger 테스트 순서
                1) /api/auth/login 또는 소셜 콜백으로 JWT 발급
                2) Swagger 우측 상단 Authorize 버튼 → Bearer 토큰 입력
                3) POST /api/v1/fcm/token/me 로 내 디바이스 토큰 등록
                4) POST /api/v1/fcm/test/me 로 테스트 알림 발송
                """
)
@SecurityRequirement(name = "bearerAuth")
public class FcmController {

    private final FcmService fcmService;
    private final FcmTokenService fcmTokenService;
    private final FcmDeviceTokenRepository tokenRepository;

    @Operation(
            summary = "내 디바이스 FCM 토큰 등록",
            description = """
                    로그인(JWT)한 사용자의 디바이스 FCM 토큰을 서버 DB에 저장/갱신합니다.
                    
                    - 로그인 직후 1회 호출
                    - onNewToken()으로 토큰이 갱신되면 다시 호출해서 DB 최신화
                    
                    (정책 A) 로그아웃/앱 종료 상태에서도 서버는 DB 토큰으로 푸시 발송 가능
                    """,
            responses = {
                    @ApiResponse(responseCode = "200", description = "등록 성공"),
                    @ApiResponse(responseCode = "401", description = "JWT 필요", content = @Content)
            }
    )
    @PostMapping(value = "/token/me", consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<Void> registerMyToken(
            Authentication authentication,
            @Valid @RequestBody FcmTokenRegisterRequest request
    ) {
        Long userId = (Long) authentication.getPrincipal();
        fcmTokenService.register(userId, request);
        return ResponseEntity.ok().build();
    }

    @Operation(
            summary = "내게 테스트 알림 발송",
            description = """
                    DB에 저장된 내 최신 활성 토큰으로 테스트 푸시를 발송합니다.
                    
                    Body가 없거나 title/body가 비어있으면 기본 템플릿으로 발송됩니다.
                    - title: Unimate Test
                    - body: 서버 템플릿 발송 테스트
                    """,
            responses = {
                    @ApiResponse(
                            responseCode = "200",
                            description = "FCM 호출 성공(FCM 응답 문자열 반환)",
                            content = @Content(
                                    mediaType = MediaType.TEXT_PLAIN_VALUE,
                                    examples = @ExampleObject(
                                            value = """
                                                    OK: {
                                                      "name": "projects/unimate-.../messages/0:...."
                                                    }
                                                    """
                                    )
                            )
                    ),
                    @ApiResponse(responseCode = "401", description = "JWT 필요", content = @Content),
                    @ApiResponse(responseCode = "500", description = "활성 토큰 없음/발송 실패", content = @Content)
            }
    )
    @PostMapping(value = "/test/me", produces = MediaType.TEXT_PLAIN_VALUE)
    public ResponseEntity<String> testSendToMe(
            Authentication authentication,
            @RequestBody(required = false) FcmSendRequest request
    ) throws IOException {

        Long userId = (Long) authentication.getPrincipal();

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
}
