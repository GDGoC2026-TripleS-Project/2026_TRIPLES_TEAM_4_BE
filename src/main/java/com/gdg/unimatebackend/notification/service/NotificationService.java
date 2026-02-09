package com.gdg.unimatebackend.notification.service;

import com.gdg.unimatebackend.notification.entity.Notification;
import com.gdg.unimatebackend.notification.entity.NotificationReceipt;
import com.gdg.unimatebackend.notification.repository.NotificationReceiptRepository;
import com.gdg.unimatebackend.notification.repository.NotificationRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class NotificationService {

    private final NotificationRepository notificationRepository;
    private final NotificationReceiptRepository notificationReceiptRepository;

    @Transactional
    public Notification createNotificationWithReceipts(Notification notification, List<Long> targetUserIds) {
        if (notification == null) {
            throw new IllegalArgumentException("notification is null");
        }
        Notification saved = saveNotificationIdempotent(notification);
        createReceiptsIdempotent(saved, targetUserIds);
        return saved;
    }

    @Transactional
    public Notification createNotificationWithReceipt(Notification notification, Long receiverId) {
        if (notification == null) {
            throw new IllegalArgumentException("notification is null");
        }
        if (receiverId == null) {
            throw new IllegalArgumentException("receiverId is null");
        }
        Notification saved = saveNotificationIdempotent(notification);
        createReceiptsIdempotent(saved, List.of(receiverId));
        return saved;
    }

    private Notification saveNotificationIdempotent(Notification notification) {
        try {
            Notification saved = notificationRepository.saveAndFlush(notification);
            org.slf4j.LoggerFactory.getLogger(NotificationService.class)
                    .info("[NOTI] saved. id={}, eventKey={}", saved.getId(), saved.getEventKey());
            return saved;
        } catch (DataIntegrityViolationException e) {
            org.slf4j.LoggerFactory.getLogger(NotificationService.class)
                    .error("[NOTI] save failed (DataIntegrityViolation). eventKey={}",
                            notification.getEventKey(), e);
            return notificationRepository.findByEventKey(notification.getEventKey())
                    .orElseThrow(() -> e);
        } catch (Exception e) {
            org.slf4j.LoggerFactory.getLogger(NotificationService.class)
                    .error("[NOTI] save failed. eventKey={}", notification.getEventKey(), e);
            throw e;
        }
    }

    private List<NotificationReceipt> createReceiptsIdempotent(Notification notification, List<Long> targetUserIds) {
        List<NotificationReceipt> created = new ArrayList<>();
        for (Long userId : targetUserIds) {
            if (userId == null) continue;
            boolean exists = notificationReceiptRepository
                    .findByNotificationIdAndUserId(notification.getId(), userId)
                    .isPresent();
            if (exists) continue;

            NotificationReceipt receipt = NotificationReceipt.builder()
                    .notification(notification)
                    .userId(userId)
                    .isCompleted(false)
                    .isRead(false)
                    .build();

            try {
                created.add(notificationReceiptRepository.save(receipt));
            } catch (Exception e) {
                org.slf4j.LoggerFactory.getLogger(NotificationService.class)
                        .error("[NOTI] receipt save failed. notificationId={}, userId={}",
                                notification.getId(), userId, e);
                throw e;
            }
        }
        return created;
    }
}
