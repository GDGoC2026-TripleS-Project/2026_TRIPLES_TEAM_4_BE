package com.gdg.unimatebackend.notification.repository;

import com.gdg.unimatebackend.notification.entity.NotificationReceipt;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface NotificationReceiptRepository extends JpaRepository<NotificationReceipt, Long> {
    Optional<NotificationReceipt> findByNotificationIdAndUserId(Long notificationId, Long userId);

    @Query("""
        select r
        from NotificationReceipt r
        join fetch r.notification n
        where r.userId = :userId
        order by
          case when r.processedAt is null then 0 else 1 end asc,
          r.processedAt desc,
          r.createdAt desc
    """)
    List<NotificationReceipt> findAllByUserIdWithNotification(@Param("userId") Long userId);
}
