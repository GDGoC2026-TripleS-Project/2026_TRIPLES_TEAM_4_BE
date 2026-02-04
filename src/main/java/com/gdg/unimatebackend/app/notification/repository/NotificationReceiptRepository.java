package com.gdg.unimatebackend.app.notification.repository;

import com.gdg.unimatebackend.app.notification.entity.NotificationReceipt;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface NotificationReceiptRepository extends JpaRepository<NotificationReceipt, Long> {
    Optional<NotificationReceipt> findByNotificationIdAndUserId(Long notificationId, Long userId);
}
