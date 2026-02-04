package com.gdg.unimatebackend.app.notification.controller;

import com.gdg.unimatebackend.app.notification.entity.NotificationReceipt;
import com.gdg.unimatebackend.app.notification.service.NotificationCompletionService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/notifications")
@Tag(name = "알림", description = "알림 완료 처리 API")
public class NotificationController {

    private final NotificationCompletionService notificationCompletionService;

    @Operation(
            summary = "알림 완료 처리",
            description = "알림 카드의 '확인 콕누르기' 완료 상태를 저장합니다.",
            security = @SecurityRequirement(name = "bearerAuth"),
            responses = {
                    @ApiResponse(responseCode = "200", description = "완료 처리 성공(멱등)"),
                    @ApiResponse(responseCode = "404", description = "수신자 아님 또는 알림 없음")
            }
    )
    @PostMapping("/{notificationId}/complete")
    public ResponseEntity<CompletionResponse> complete(
            Authentication authentication,
            @PathVariable Long notificationId
    ) {
        Long userId = (Long) authentication.getPrincipal();
        var opt = notificationCompletionService.complete(notificationId, userId);
        if (opt.isEmpty()) {
            return ResponseEntity.notFound().build();
        }

        NotificationReceipt receipt = opt.get();
        return ResponseEntity.ok(CompletionResponse.builder()
                .notificationId(notificationId)
                .userId(userId)
                .isCompleted(receipt.isCompleted())
                .completedAt(receipt.getCompletedAt())
                .build());
    }

    @Getter
    @Builder
    @AllArgsConstructor
    public static class CompletionResponse {
        private Long notificationId;
        private Long userId;
        private boolean isCompleted;
        private LocalDateTime completedAt;
    }
}
