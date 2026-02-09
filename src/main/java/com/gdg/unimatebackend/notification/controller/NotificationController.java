package com.gdg.unimatebackend.notification.controller;

import com.gdg.unimatebackend.notification.dto.NotificationItemResponse;
import com.gdg.unimatebackend.notification.entity.NotificationReceipt;
import com.gdg.unimatebackend.notification.service.NotificationCompletionService;
import com.gdg.unimatebackend.notification.service.NotificationQueryService;
import com.gdg.unimatebackend.notification.service.DdayNotificationService;
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
    private final NotificationQueryService notificationQueryService;
    private final DdayNotificationService ddayNotificationService;

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

    @Operation(
            summary = "내 알림 목록 조회",
            description = "알림 목록을 조회합니다. 처리되지 않은 알림이 먼저 오고, 처리된 알림은 processedAt desc로 정렬됩니다.",
            security = @SecurityRequirement(name = "bearerAuth")
    )
    @GetMapping
    public ResponseEntity<java.util.List<NotificationItemResponse>> list(Authentication authentication) {
        Long userId = (Long) authentication.getPrincipal();
        return ResponseEntity.ok(notificationQueryService.getMyNotifications(userId));
    }

    @Operation(
            summary = "DDAY 테스트 실행(임시)",
            description = "DDAY 스케줄러 로직을 즉시 실행합니다.",
            security = @SecurityRequirement(name = "bearerAuth")
    )
    @PostMapping("/dday-test/run")
    public ResponseEntity<java.util.Map<String, String>> runDdayTest(Authentication authentication) {
        ddayNotificationService.generateDailyDdays();
        return ResponseEntity.ok(java.util.Map.of("status", "ok"));
    }

    @Operation(
            summary = "알림 읽음 처리",
            description = "isRead=true, processedAt=now. action=true인 알림은 actionDone 이후에만 처리됩니다.",
            security = @SecurityRequirement(name = "bearerAuth")
    )
    @PatchMapping("/{notificationId}/read")
    public ResponseEntity<NotificationItemResponse> markRead(
            Authentication authentication,
            @PathVariable Long notificationId
    ) {
        Long userId = (Long) authentication.getPrincipal();
        return ResponseEntity.ok(notificationQueryService.markRead(notificationId, userId));
    }

    @Operation(
            summary = "알림 전체 읽음 처리(버튼 없는 알림만)",
            description = "action=false 알림만 전체 읽음 처리합니다.",
            security = @SecurityRequirement(name = "bearerAuth")
    )
    @PatchMapping("/read-all")
    public ResponseEntity<java.util.Map<String, Integer>> readAll(Authentication authentication) {
        Long userId = (Long) authentication.getPrincipal();
        int count = notificationQueryService.readAllNonAction(userId);
        return ResponseEntity.ok(java.util.Map.of("updatedCount", count));
    }

    @Operation(
            summary = "알림 액션 완료 처리",
            description = "actionDone=true만 변경합니다. read/processedAt은 변경하지 않습니다.",
            security = @SecurityRequirement(name = "bearerAuth")
    )
    @PatchMapping("/{notificationId}/action-done")
    public ResponseEntity<NotificationItemResponse> actionDone(
            Authentication authentication,
            @PathVariable Long notificationId
    ) {
        Long userId = (Long) authentication.getPrincipal();
        return ResponseEntity.ok(notificationQueryService.markActionDone(notificationId, userId));
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
