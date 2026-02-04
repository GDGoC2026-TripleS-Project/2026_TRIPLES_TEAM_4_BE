package com.gdg.unimatebackend.app.notification.service;

import com.gdg.unimatebackend.app.notification.entity.NotificationReceipt;
import com.gdg.unimatebackend.app.notification.repository.NotificationReceiptRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class NotificationCompletionService {

    private final NotificationReceiptRepository notificationReceiptRepository;

    @Transactional
    public Optional<NotificationReceipt> complete(Long notificationId, Long userId) {
        Optional<NotificationReceipt> opt = notificationReceiptRepository
                .findByNotificationIdAndUserId(notificationId, userId);

        if (opt.isEmpty()) {
            return Optional.empty();
        }

        NotificationReceipt receipt = opt.get();
        if (!receipt.isCompleted()) {
            receipt.markCompleted(LocalDateTime.now());
        }

        return Optional.of(receipt);
    }
}
