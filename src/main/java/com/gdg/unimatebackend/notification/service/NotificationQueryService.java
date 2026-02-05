package com.gdg.unimatebackend.notification.service;

import com.gdg.unimatebackend.notification.dto.NotificationItemResponse;
import com.gdg.unimatebackend.notification.entity.Notification;
import com.gdg.unimatebackend.notification.entity.NotificationReceipt;
import com.gdg.unimatebackend.notification.repository.NotificationReceiptRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class NotificationQueryService {

    private final NotificationReceiptRepository notificationReceiptRepository;

    private static final Set<String> ACTION_TYPES = Set.of("POKE", "MEETING_REQUEST");

    @Transactional(readOnly = true)
    public List<NotificationItemResponse> getMyNotifications(Long userId) {
        return notificationReceiptRepository.findAllByUserIdWithNotification(userId).stream()
                .map(this::toItem)
                .toList();
    }

    @Transactional
    public NotificationItemResponse markRead(Long notificationId, Long userId) {
        NotificationReceipt receipt = notificationReceiptRepository
                .findByNotificationIdAndUserId(notificationId, userId)
                .orElseThrow(() -> new IllegalArgumentException("알림을 찾을 수 없습니다"));

        boolean action = isActionType(receipt.getNotification());
        if (action && !receipt.isCompleted()) {
            throw new IllegalArgumentException("actionDone 이후에만 읽음 처리할 수 있습니다");
        }

        receipt.markRead(LocalDateTime.now());
        return toItem(receipt);
    }

    @Transactional
    public int readAllNonAction(Long userId) {
        List<NotificationReceipt> receipts = notificationReceiptRepository.findAllByUserIdWithNotification(userId);
        int updated = 0;
        LocalDateTime now = LocalDateTime.now();
        for (NotificationReceipt r : receipts) {
            boolean action = isActionType(r.getNotification());
            if (action) continue;
            if (!r.isRead()) {
                r.markRead(now);
                updated++;
            }
        }
        return updated;
    }

    @Transactional
    public NotificationItemResponse markActionDone(Long notificationId, Long userId) {
        NotificationReceipt receipt = notificationReceiptRepository
                .findByNotificationIdAndUserId(notificationId, userId)
                .orElseThrow(() -> new IllegalArgumentException("알림을 찾을 수 없습니다"));

        receipt.markCompleted(LocalDateTime.now());
        return toItem(receipt);
    }

    private boolean isActionType(Notification notification) {
        if (notification == null) return false;
        String type = notification.getType();
        return type != null && ACTION_TYPES.contains(type);
    }

    private NotificationItemResponse toItem(NotificationReceipt r) {
        Notification n = r.getNotification();
        boolean action = isActionType(n);
        return NotificationItemResponse.builder()
                .id(n.getId())
                .type(n.getType())
                .alarmType(n.getAlarmType())
                .teamId(n.getTeamId())
                .teamName(n.getTeamName())
                .teamColorHex(n.getTeamColorHex())
                .messageTitle(n.getMessageTitle())
                .messageBody(n.getMessageBody())
                .createdAt(n.getCreatedAt())
                .isRead(r.isRead())
                .processedAt(r.getProcessedAt())
                .action(action)
                .actionDone(r.isCompleted())
                .build();
    }
}
