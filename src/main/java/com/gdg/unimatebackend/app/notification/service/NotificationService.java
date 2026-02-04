package com.gdg.unimatebackend.app.notification.service;

import com.gdg.unimatebackend.app.notification.entity.Notification;
import com.gdg.unimatebackend.app.notification.entity.NotificationReceipt;
import com.gdg.unimatebackend.app.notification.repository.NotificationReceiptRepository;
import com.gdg.unimatebackend.app.notification.repository.NotificationRepository;
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
        Notification saved = saveNotificationIdempotent(notification);
        createReceiptsIdempotent(saved, targetUserIds);
        return saved;
    }

    private Notification saveNotificationIdempotent(Notification notification) {
        try {
            return notificationRepository.save(notification);
        } catch (DataIntegrityViolationException e) {
            return notificationRepository.findByEventKey(notification.getEventKey())
                    .orElseThrow(() -> e);
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
                    .build();

            created.add(notificationReceiptRepository.save(receipt));
        }
        return created;
    }
}
